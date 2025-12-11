#ifndef LANG_DETECT_JNI_HPP
#define LANG_DETECT_JNI_HPP

#include <jni.h>

#ifdef __cplusplus
extern "C" {
    #endif

    JNIEXPORT jobject JNICALL Java_com_ezeksapps_ezeksapp_jni_LangDetectJNI_detectLang
    (JNIEnv *, jobject, jstring, jstring);

    #ifdef __cplusplus
}
#endif

#endif // LANG_DETECT_JNI_HPP
