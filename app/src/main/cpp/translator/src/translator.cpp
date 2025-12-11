#include <android/log.h>
#include <sstream>
#include <fstream>
#include <vector>
#include <algorithm>
#include "translator.hpp"
#include <ctranslate2/translator.h>

/* USE THESE FOR Android.Log FUNCTIONS */
#define LOG_TAG "TranslationJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)


/* HISTORY OF THIS FILE:
 * Originally, I wanted to use pure Marian-NMT for this translation engine, I realised that Marian-dev
 * didn't build well for ARM platforms though & was too closely tied to x86. I found another offline translator
 * app that used bergamot-translator, which wrapped a more easily built version of marian, initially this was fine
 * as I was using the firefox-translation-models for 'lite' models, however on attempting to introduce full models
 * which were going to be the OPUS-MT ones I realised bergamot wasn't capable of handling them & it would overcomplicate the
 * system. This prompted a switch to CTranslate2 + SentencePiece, lite models are now just INT8 quantised versions of the
 * OPUS-MT models */

bool TranslationEngine::init(const std::string& model_dir) {
    try {
        LOGI("Initializing CTranslate2 model from: %s", model_dir.c_str());

        // Load translator (all models are now CTranslate2 format)
        translator = std::make_unique<ctranslate2::Translator>(model_dir);
        if (!translator) {
            LOGE("Failed to create CTranslate2 translator");
            return false;
        }

        // Load SentencePiece tokenizers - handle both shared and separate vocabularies
        sp_source = std::make_unique<sentencepiece::SentencePieceProcessor>();
        sp_target = std::make_unique<sentencepiece::SentencePieceProcessor>();

        // Find vocabulary files
        std::string source_vocab, target_vocab;

        // Try to find separate source/target vocabularies first
        std::ifstream source_test(model_dir + "/source.spm");
        std::ifstream target_test(model_dir + "/target.spm");

        if (source_test.good() && target_test.good()) {
            // Case 1: Separate source and target vocabularies
            source_vocab = model_dir + "/source.spm";
            target_vocab = model_dir + "/target.spm";
            LOGI("Found separate vocabularies: source.spm and target.spm");
            has_separate_vocabs = true;
        } else {
            // Case 2: Shared vocabulary - try common names
            std::vector<std::string> shared_candidates = {
                    model_dir + "/vocab.spm",
                    model_dir + "/sentencepiece.model",
                    model_dir + "/spm.model",
                    model_dir + "/source.spm",  // Fallback to source if target doesn't exist
                    model_dir + "/target.spm"   // Or target if source doesn't exist
            };

            for (const auto& candidate : shared_candidates) {
                std::ifstream test_file(candidate);
                if (test_file.good()) {
                    source_vocab = candidate;
                    target_vocab = candidate;
                    LOGI("Using shared vocabulary: %s", candidate.c_str());
                    has_separate_vocabs = false;
                    break;
                }
            }
        }

        if (source_vocab.empty() || target_vocab.empty()) {
            LOGE("No vocabulary files found in model directory");
            return false;
        }

        // Load the tokenizer(s)
        auto status = sp_source->Load(source_vocab);
        if (!status.ok()) {
            LOGE("Failed to load source SentencePiece: %s", status.ToString().c_str());
            return false;
        }

        status = sp_target->Load(target_vocab);
        if (!status.ok()) {
            LOGE("Failed to load target SentencePiece: %s", status.ToString().c_str());
            return false;
        }

        LOGI("Source vocabulary size: %d", sp_source->GetPieceSize());
        LOGI("Target vocabulary size: %d", sp_target->GetPieceSize());

        if (!has_separate_vocabs) {
            LOGI("Using shared vocabulary for both source and target");
        }

        ready = true;
        LOGI("Engine initialization successful");
        return true;

    } catch (const std::exception& e) {
        LOGE("Exception in init: %s", e.what());
        return false;
    }
}

std::string TranslationEngine::translate(const std::string& input_text) const {
    if (!ready || !translator || !sp_source || !sp_target) {
        LOGE("Engine not ready");
        return "ERROR: Engine not ready";
    }

    if (input_text.empty()) {
        return "";
    }

    LOGD("Translating: '%s'", input_text.c_str());

    try {
        // 1. TOKENIZE INPUT - always use source tokenizer
        std::vector<std::string> tokens;
        sp_source->Encode(input_text, &tokens);

        // Add end-of-sequence token (CTranslate2 expects this)
        tokens.emplace_back("</s>");

        LOGD("Tokenized: %zu tokens (+ </s>)", tokens.size());

        // Debug first few tokens
        if (!tokens.empty()) {
            std::string token_str;
            for (size_t i = 0; i < std::min(tokens.size(), size_t(5)); i++) {
                token_str += "'" + tokens[i] + "' ";
            }
            LOGD("Input tokens: %s", token_str.c_str());
        }

        // 2. TRANSLATE
        ctranslate2::TranslationOptions options;
        options.max_decoding_length = 100;
        options.beam_size = 4;
        options.repetition_penalty = 1.5;
        options.no_repeat_ngram_size = 3;
        options.end_token = "</s>";
        options.return_end_token = false;
        options.min_decoding_length = 1;
        options.disable_unk = true;

        LOGD("Starting translation...");
        auto results = translator->translate_batch({tokens}, options);

        if (results.empty() || results[0].output().empty()) {
            LOGE("Empty translation result");
            return "ERROR: Empty result";
        }

        // 3. DECODE OUTPUT - always use target tokenizer
        const auto& output_tokens = results[0].output();
        LOGD("Got %zu output tokens", output_tokens.size());

        // Clean special tokens from output
        std::vector<std::string> clean_tokens;
        for (const auto& token : output_tokens) {
            if (token != "<s>" && token != "<pad>" && token != "</s>") {
                clean_tokens.push_back(token);
            }
        }

        if (clean_tokens.empty()) {
            LOGW("No tokens after cleaning");
            return "";
        }

        // Use target tokenizer for decoding
        std::string decoded_text;
        auto status = sp_target->Decode(clean_tokens, &decoded_text);
        if (!status.ok()) {
            LOGE("Failed to decode: %s", status.ToString().c_str());
            return "ERROR: Decoding failed";
        }

        // 4. POST-PROCESSING
        std::string final_text;
        bool last_was_space = false;

        for (size_t i = 0; i < decoded_text.length(); i++) {
            // Check for "▁" (U+2581) - UTF-8 bytes: 0xE2 0x96 0x81
            if (i + 2 < decoded_text.length() &&
                static_cast<unsigned char>(decoded_text[i]) == 0xE2 &&
                static_cast<unsigned char>(decoded_text[i+1]) == 0x96 &&
                static_cast<unsigned char>(decoded_text[i+2]) == 0x81) {

                if (!last_was_space && !final_text.empty()) {
                    final_text.push_back(' ');
                }
                last_was_space = true;
                i += 2;
            } else {
                final_text.push_back(decoded_text[i]);
                last_was_space = false;
            }
        }

        // Trim whitespace
        size_t start = final_text.find_first_not_of(" \t\n\r");
        size_t end = final_text.find_last_not_of(" \t\n\r");

        if (start == std::string::npos || end == std::string::npos) {
            final_text = "";
        } else {
            final_text = final_text.substr(start, end - start + 1);
        }

        LOGI("TRANSLATION: '%s' -> '%s'", input_text.c_str(), final_text.c_str());
        return final_text;

    } catch (const std::exception& e) {
        LOGE("Exception in translate: %s", e.what());
        return "ERROR: " + std::string(e.what());
    }
}