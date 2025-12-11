#ifndef TRANSLATOR_HPP
#define TRANSLATOR_HPP

#include <string>
#include <memory>
#include <ctranslate2/translator.h>
#include <sentencepiece/src/sentencepiece_processor.h>

class TranslationEngine {
public:
    bool init(const std::string& model_dir);
    std::string translate(const std::string& input_text) const;

private:
    bool ready = false;
    bool has_separate_vocabs = false;
    std::unique_ptr<ctranslate2::Translator> translator;

    // Sentencepiece Models (.spm)
    std::unique_ptr<sentencepiece::SentencePieceProcessor> sp_source;
    std::unique_ptr<sentencepiece::SentencePieceProcessor> sp_target;
};

#endif