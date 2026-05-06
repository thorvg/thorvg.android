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
#include "SvgData.h"

extern "C" jlong
Java_org_thorvg_core_svg_SvgNativeBindings_nCreateSvg(JNIEnv *env, jclass clazz,
        jstring content, jint length, jintArray out_values) {
    if (content == nullptr) {
        return 0;
    }

    if (tvg::Initializer::init(3) != tvg::Result::Success) {
        return 0;
    }

    const char* inputStr = env->GetStringUTFChars(content, nullptr);
    auto* newData = new SvgComposition::Data(inputStr, length);
    env->ReleaseStringUTFChars(content, inputStr);

    jint* svgInfo = env->GetIntArrayElements(out_values, nullptr);
    if (svgInfo != nullptr) {
        svgInfo[0] = static_cast<jint>(newData->mWidth);
        svgInfo[1] = static_cast<jint>(newData->mHeight);
        env->ReleaseIntArrayElements(out_values, svgInfo, 0);
    }

    return reinterpret_cast<jlong>(newData);
}

extern "C" void
Java_org_thorvg_core_svg_SvgNativeBindings_nDestroySvg(JNIEnv* env, jclass clazz, jlong svg_ptr) {
    tvg::Initializer::term();

    if (svg_ptr == 0) {
        return;
    }

    auto* data = reinterpret_cast<SvgComposition::Data*>(svg_ptr);
    delete data;
}

extern "C" void
Java_org_thorvg_core_svg_SvgNativeBindings_nSetSvgBufferSize(JNIEnv* env, jclass clazz,
        jlong svg_ptr, jobject bitmap, jfloat width, jfloat height) {
    if (svg_ptr == 0) {
        return;
    }

    auto* data = reinterpret_cast<SvgComposition::Data*>(svg_ptr);
    void *buffer;
    if (AndroidBitmap_lockPixels(env, bitmap, &buffer) >= 0) {
        data->setBufferSize((uint32_t *) buffer, width, height);
        AndroidBitmap_unlockPixels(env, bitmap);
    }
}

extern "C" void
Java_org_thorvg_core_svg_SvgNativeBindings_nDrawSvg(JNIEnv* env, jclass clazz,
        jlong svg_ptr, jobject bitmap) {
    if (svg_ptr == 0) {
        return;
    }

    auto* data = reinterpret_cast<SvgComposition::Data*>(svg_ptr);
    void *buffer;
    if (AndroidBitmap_lockPixels(env, bitmap, &buffer) >= 0) {
        data->draw();
        AndroidBitmap_unlockPixels(env, bitmap);
    }
}
