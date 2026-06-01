/*
 * Copyright (c) 2024 - 2026 ThorVG project. All rights reserved.

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#include <android/bitmap.h>
#include <thorvg.h>
#include <jni.h>
#include "LottieData.h"

using namespace std;

static jlong _createLottie(JNIEnv *env, jstring content, jintArray out_values,
        LottieDrawable::Data* (*createData)(JNIEnv* env, jstring content)) {
    if (content == nullptr) {
        return 0;
    }

    if (tvg::Initializer::init(3) != tvg::Result::Success) {
        return 0;
    }

    auto* data = createData(env, content);
    if (data == nullptr || !data->valid()) {
        delete data;
        tvg::Initializer::term();
        return 0;
    }

    if (out_values != nullptr) {
        jint* contentInfo = env->GetIntArrayElements(out_values, nullptr);
        if (contentInfo != nullptr) {
            contentInfo[0] = (jint) data->mAnimation->totalFrame();
            contentInfo[1] = (jint) (data->mAnimation->duration() * 1000.0f);
            env->ReleaseIntArrayElements(out_values, contentInfo, 0);
        }
    }

    return reinterpret_cast<jlong>(data);
}

extern "C" jlong
Java_org_thorvg_core_lottie_LottieNativeBindings_nCreateSwLottie(JNIEnv *env, jclass clazz,
        jstring content, jintArray out_values) {
    return _createLottie(env, content, out_values, [](JNIEnv* env, jstring content) -> LottieDrawable::Data* {
        return new LottieDrawable::SwData(env, content);
    });
}

extern "C" jlong
Java_org_thorvg_core_lottie_LottieNativeBindings_nCreateGlLottie(JNIEnv *env, jclass clazz,
        jstring content, jintArray out_values) {
    return _createLottie(env, content, out_values, [](JNIEnv* env, jstring content) -> LottieDrawable::Data* {
        return new LottieDrawable::GlData(env, content);
    });
}

extern "C" void
Java_org_thorvg_core_lottie_LottieNativeBindings_nDestroyLottie(JNIEnv* env, jclass clazz, jlong lottie_ptr) {
    if (lottie_ptr == 0) {
        return;
    }

    auto* data = reinterpret_cast<LottieDrawable::Data*>(lottie_ptr);
    delete data;
    tvg::Initializer::term();
}

extern "C" void
Java_org_thorvg_core_lottie_LottieNativeBindings_nResizeSwLottie(JNIEnv* env, jclass clazz,
        jlong lottie_ptr, jobject bitmap, jfloat width, jfloat height) {
    if (lottie_ptr == 0) {
        return;
    }

    auto* data = reinterpret_cast<LottieDrawable::SwData*>(lottie_ptr);
    void *buffer;
    if (AndroidBitmap_lockPixels(env, bitmap, &buffer) >= 0) {
        data->resize((uint32_t *) buffer, width, height);
        AndroidBitmap_unlockPixels(env, bitmap);
    }
}

extern "C" jboolean
Java_org_thorvg_core_lottie_LottieNativeBindings_nResizeGlLottie(JNIEnv* env, jclass clazz,
        jlong lottie_ptr, jlong display, jlong surface, jlong context, jint framebuffer_id,
        jfloat width, jfloat height) {
    if (lottie_ptr == 0) {
        return JNI_FALSE;
    }

    auto* data = reinterpret_cast<LottieDrawable::GlData*>(lottie_ptr);
    auto result = data->resize(
            reinterpret_cast<void*>(display),
            reinterpret_cast<void*>(surface),
            reinterpret_cast<void*>(context),
            framebuffer_id,
            width,
            height);
    return result ? JNI_TRUE : JNI_FALSE;
}

extern "C" void
Java_org_thorvg_core_lottie_LottieNativeBindings_nDrawSwLottieFrame(JNIEnv* env, jclass clazz,
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

extern "C" void
Java_org_thorvg_core_lottie_LottieNativeBindings_nDrawGlLottieFrame(JNIEnv* env, jclass clazz,
        jlong lottie_ptr, jint frame) {
    if (lottie_ptr == 0) {
        return;
    }

    auto* data = reinterpret_cast<LottieDrawable::Data*>(lottie_ptr);
    data->draw(frame);
}
