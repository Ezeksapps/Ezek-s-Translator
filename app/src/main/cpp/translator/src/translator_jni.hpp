#ifndef TRANSLATOR_JNI_HPP
#define TRANSLATOR_JNI_HPP

#include <jni.h>
#include "translator.hpp"

#ifdef __cplusplus
extern "C" {
#endif

// Constructor equivalent - creates a new C++ TranslationEngine instance
JNIEXPORT jlong JNICALL
Java_com_ezeksapps_ezeksapp_jni_TranslatorJNI_createNativeInstance(JNIEnv *env, jobject obj);

// Initialises the native engine with model directory
JNIEXPORT jboolean JNICALL
Java_com_ezeksapps_ezeksapp_jni_TranslatorJNI_initNativeEngine(JNIEnv *env, jobject obj,
                                                                     jlong engine_ptr, jstring model_dir);

// Performs translation
JNIEXPORT jstring JNICALL
Java_com_ezeksapps_ezeksapp_jni_TranslatorJNI_runTranslation(JNIEnv *env, jobject obj,
                                                                 jlong engine_ptr, jstring text);

// Destructor equivalent - deletes the C++ TranslationEngine instance
JNIEXPORT void JNICALL
Java_com_ezeksapps_ezeksapp_jni_TranslatorJNI_deleteNativeInstance(JNIEnv *env, jobject obj,
                                                                   jlong engine_ptr);

#ifdef __cplusplus
}
#endif

#endif // TRANSLATOR_JNI_HPP