#include <android/bitmap.h>
#include <thorvg.h>
#include <jni.h>
#include "LottieData.h"

using namespace std;

extern "C" jlong
Java_org_thorvg_lottie_LottieDrawable_nCreateLottie(JNIEnv *env, jclass clazz,
        jstring content, jint length, jintArray out_values) {
    if (tvg::Initializer::init(tvg::CanvasEngine::Sw, 3) != tvg::Result::Success) {
        return 0;
    }

    const char* inputStr = env->GetStringUTFChars(content, nullptr);
    auto* newData = new LottieDrawable::Data(inputStr, length);
    env->ReleaseStringUTFChars(content, inputStr);

    jint* contentInfo = env->GetIntArrayElements(out_values, nullptr);
    if (contentInfo != nullptr) {
        contentInfo[0] = (jint) newData->mAnimation->totalFrame();
        contentInfo[1] = (jint) newData->mAnimation->duration();
        env->ReleaseIntArrayElements(out_values, contentInfo, 0);
    }

    return reinterpret_cast<jlong>(newData);
}

extern "C" void
Java_org_thorvg_lottie_LottieDrawable_nDestroyLottie(JNIEnv* env, jclass clazz, jlong lottie_ptr) {
    tvg::Initializer::term(tvg::CanvasEngine::Sw);

    if (lottie_ptr == 0) {
        return;
    }

    auto* data = reinterpret_cast<LottieDrawable::Data*>(lottie_ptr);
    delete data;
}

extern "C" void
Java_org_thorvg_lottie_LottieDrawable_nSetLottieBufferSize(JNIEnv* env, jclass clazz,
        jlong lottie_ptr, jobject bitmap, jfloat width, jfloat height) {
    if (lottie_ptr == 0) {
        return;
    }

    auto* data = reinterpret_cast<LottieDrawable::Data*>(lottie_ptr);
    void *buffer;
    if (AndroidBitmap_lockPixels(env, bitmap, &buffer) >= 0) {
        data->setBufferSize((uint32_t *) buffer, width, height);
        AndroidBitmap_unlockPixels(env, bitmap);
    }
}

extern "C" void
Java_org_thorvg_lottie_LottieDrawable_nDrawLottieFrame(JNIEnv* env, jclass clazz,
        jlong lottie_ptr, jobject bitmap, jint frame) {
    if (lottie_ptr == 0) {
        return;
    }

    auto* data = reinterpret_cast<LottieDrawable::Data*>(lottie_ptr);
    void *buffer;
    if (AndroidBitmap_lockPixels(env, bitmap, &buffer) >= 0) {
        data->draw(frame);
        AndroidBitmap_unlockPixels(env, bitmap);
    }
}