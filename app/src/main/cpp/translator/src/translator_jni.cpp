#include "translator_jni.hpp"
#include <android/log.h>

#define LOG_TAG "TranslationJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_ezeksapps_ezeksapp_jni_TranslatorJNI_createNativeInstance(JNIEnv *env, jobject obj) {
    TranslationEngine *engine = nullptr;
    try {
        engine = new TranslationEngine();
        //LOGI("C++ TranslationEngine created: %p", engine);
        return reinterpret_cast<jlong>(engine);
    } catch (const std::exception &e) {
        //LOGE("Failed to create C++ TranslationEngine: %s", e.what());
        if (engine) delete engine;
        return 0;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_ezeksapps_ezeksapp_jni_TranslatorJNI_initNativeEngine(JNIEnv *env, jobject obj,
                                                               jlong engine_ptr, jstring model_dir) {
    if (engine_ptr == 0) {
        //LOGE("Invalid engine pointer");
        return JNI_FALSE;
    }

    const char *model_dir_str = env->GetStringUTFChars(model_dir, nullptr);

    if (!model_dir_str) {
        //LOGE("Failed to get string parameters");
        // Clean up any allocated strings
        if (model_dir_str) env->ReleaseStringUTFChars(model_dir, model_dir_str);
        return JNI_FALSE;
    }

    bool success = false;
    try {
        auto *engine = reinterpret_cast<TranslationEngine *>(engine_ptr);
        std::string model_path(model_dir_str);

        //LOGI("Init engine with model: %s, %s->%s",
             //model_path.c_str(), source_lang_cpp.c_str(), target_lang_cpp.c_str());

        success = engine->init(model_path);

        if (success) {
            //LOGI("Engine init success");
        } else {
            //LOGE("Engine init failed");
        }
    } catch (const std::exception &e) {
       // LOGE("Exception in init: %s", e.what());
    }

    env->ReleaseStringUTFChars(model_dir, model_dir_str);
    return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_ezeksapps_ezeksapp_jni_TranslatorJNI_runTranslation(JNIEnv *env, jobject obj,
                                                             jlong engine_ptr, jstring text) {
    if (engine_ptr == 0) {
        //LOGE("Invalid engine pointer");
        return env->NewStringUTF("ERROR: Engine not init");
    }

    const char *text_str = env->GetStringUTFChars(text, nullptr);
    if (!text_str) {
        //LOGE("Failed to get input text");
        return env->NewStringUTF("ERROR: Invalid input");
    }

    jstring result_str = nullptr;
    try {
        auto *engine = reinterpret_cast<TranslationEngine *>(engine_ptr);
        std::string input(text_str);
        std::string result = engine->translate(input);
        result_str = env->NewStringUTF(result.c_str());
    } catch (const std::exception &e) {
        //LOGE("Exception in translate: %s", e.what());
        result_str = env->NewStringUTF("ERROR: Translation failed");
    }

    env->ReleaseStringUTFChars(text, text_str);
    return result_str;
}

JNIEXPORT void JNICALL
Java_com_ezeksapps_ezeksapp_jni_TranslatorJNI_deleteNativeInstance(JNIEnv *env, jobject obj,
                                                                   jlong engine_ptr) {
    if (engine_ptr != 0) {
        auto *engine = reinterpret_cast<TranslationEngine *>(engine_ptr);
        //LOGI("Deleting C++ TranslationEngine: %p", engine);
        delete engine;
    }
}

} // extern "C"
