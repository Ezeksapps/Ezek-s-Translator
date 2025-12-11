#include <android/log.h>
#include <jni.h>
#include "compact_lang_det.h"

/* credit to davidv for this implementation (this is practically a copy of the lang detector there) */

struct DetectionResult {
    std::string lang;
    bool is_reliable;
    int confidence;
};

DetectionResult detectLang(const char* text, const char* lang_hint = nullptr) {
    bool is_reliable;
    int text_bytes = (int) strlen(text);
    bool is_plain_text = true;

    CLD2::Language hint_lang = CLD2::UNKNOWN_LANGUAGE;
    if (lang_hint != nullptr && strlen(lang_hint) > 0) {
        hint_lang = CLD2::GetLanguageFromName(lang_hint);
    }

    /* This part of the code could be simplified to remove the top 3s & just grab the top most likely language,
     * but doing so doesn't allow the confidence score to be calculated */

    CLD2::CLDHints hints = {nullptr, nullptr, 0, hint_lang};
    CLD2::Language lang_top3[3]; // top 3 detected languages
    int confidence_percents[3]; // confidence %s for langs
    double normalized_scores[3];
    int chunk_bytes;

    CLD2::ExtDetectLanguageSummary(
            text,
            text_bytes,
            is_plain_text,
            &hints,
            0,
            lang_top3,
            confidence_percents,
            normalized_scores,
            nullptr,
            &chunk_bytes,
            &is_reliable
    );

    return DetectionResult{
        // return top most likely language
            CLD2::LanguageCode(lang_top3[0]),
            is_reliable,
            confidence_percents[0]
    };
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_ezeksapps_ezeksapp_jni_LangDetectJNI_detectLang(
    JNIEnv *env,
    jobject obj,
    jstring text,
    jstring language_hint) {

    // Convert Java strings to C strings
    const char *native_text = env->GetStringUTFChars(text, nullptr);
    const char *native_hint = nullptr;

    if (language_hint != nullptr) {
        native_hint = env->GetStringUTFChars(language_hint, nullptr);
    }

    DetectionResult result = detectLang(native_text, native_hint);

    // Release string memory
    env->ReleaseStringUTFChars(text, native_text);
    if (language_hint != nullptr) {
        env->ReleaseStringUTFChars(language_hint, native_hint);
    }

    // Create Java DetectionResult object
    jclass result_class = env->FindClass("com/ezeksapps/ezeksapp/jni/DetectionResult");
    if (result_class == nullptr) {
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(result_class, "<init>", "(Ljava/lang/String;ZI)V");
    if (constructor == nullptr) {
        return nullptr;
    }

    jstring java_lang = env->NewStringUTF(result.lang.c_str());
    jobject java_result = env->NewObject(result_class, constructor, java_lang, result.is_reliable, result.confidence);

    return java_result;
    }
