/*
 * Copyright (c) 2026 ThorVG project. All rights reserved.

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
#include <jni.h>
#include "VideoData.h"

static jlong _create(JNIEnv* env, VideoComposition::Data* data, jintArray outValues)
{
    if (!data->valid()) {
        delete data;
        tvg::Initializer::term();
        return 0;
    }

    if (outValues) {
        auto count = env->GetArrayLength(outValues);
        auto info = env->GetIntArrayElements(outValues, nullptr);
        if (info) {
            if (count > 0) info[0] = static_cast<jint>(data->mWidth);
            if (count > 1) info[1] = static_cast<jint>(data->mHeight);
            if (count > 2) info[2] = static_cast<jint>(data->mDuration * 1000.0f);
            if (count > 3) info[3] = static_cast<jint>(data->mFrameDuration * 1000.0f + 0.5f);
            env->ReleaseIntArrayElements(outValues, info, 0);
        }
    }

    return reinterpret_cast<jlong>(data);
}

extern "C" jlong
Java_org_thorvg_core_video_VideoNativeBindings_nCreateVideo(JNIEnv* env, jclass clazz,
        jstring path, jintArray out_values)
{
    if (!path) return 0;

    if (tvg::Initializer::init(3) != tvg::Result::Success) return 0;

    auto input = env->GetStringUTFChars(path, nullptr);
    if (!input) {
        tvg::Initializer::term();
        return 0;
    }

    auto data = new VideoComposition::Data(input);
    env->ReleaseStringUTFChars(path, input);
    return _create(env, data, out_values);
}

extern "C" jlong
Java_org_thorvg_core_video_VideoNativeBindings_nCreateVideoData(JNIEnv* env, jclass clazz,
        jbyteArray bytes, jint size, jintArray out_values)
{
    if (!bytes || size <= 0 || size > env->GetArrayLength(bytes)) return 0;

    if (tvg::Initializer::init(3) != tvg::Result::Success) return 0;

    auto input = env->GetByteArrayElements(bytes, nullptr);
    if (!input) {
        tvg::Initializer::term();
        return 0;
    }

    auto data = new VideoComposition::Data(reinterpret_cast<const char*>(input), static_cast<uint32_t>(size));
    env->ReleaseByteArrayElements(bytes, input, JNI_ABORT);
    return _create(env, data, out_values);
}

extern "C" void
Java_org_thorvg_core_video_VideoNativeBindings_nSetVideoBufferSize(JNIEnv* env, jclass clazz,
        jlong video_ptr, jobject bitmap, jfloat width, jfloat height)
{
    if (!video_ptr) return;

    auto* data = reinterpret_cast<VideoComposition::Data*>(video_ptr);
    void* buffer;
    if (AndroidBitmap_lockPixels(env, bitmap, &buffer) >= 0) {
        data->setBufferSize(reinterpret_cast<uint32_t*>(buffer), width, height);
        AndroidBitmap_unlockPixels(env, bitmap);
    }
}

extern "C" void
Java_org_thorvg_core_video_VideoNativeBindings_nDrawVideo(JNIEnv* env, jclass clazz,
        jlong video_ptr, jobject bitmap)
{
    if (!video_ptr) return;

    auto* data = reinterpret_cast<VideoComposition::Data*>(video_ptr);
    void* buffer;
    if (AndroidBitmap_lockPixels(env, bitmap, &buffer) >= 0) {
        data->draw(reinterpret_cast<uint32_t*>(buffer));
        AndroidBitmap_unlockPixels(env, bitmap);
    }
}

extern "C" void
Java_org_thorvg_core_video_VideoNativeBindings_nPlayVideo(JNIEnv* env, jclass clazz, jlong video_ptr)
{
    if (video_ptr) reinterpret_cast<VideoComposition::Data*>(video_ptr)->mVideo->play();
}

extern "C" void
Java_org_thorvg_core_video_VideoNativeBindings_nPauseVideo(JNIEnv* env, jclass clazz, jlong video_ptr)
{
    if (video_ptr) reinterpret_cast<VideoComposition::Data*>(video_ptr)->mVideo->pause();
}

extern "C" void
Java_org_thorvg_core_video_VideoNativeBindings_nStopVideo(JNIEnv* env, jclass clazz, jlong video_ptr)
{
    if (video_ptr) reinterpret_cast<VideoComposition::Data*>(video_ptr)->mVideo->stop();
}

extern "C" void
Java_org_thorvg_core_video_VideoNativeBindings_nSeekVideo(JNIEnv* env, jclass clazz,
        jlong video_ptr, jlong time_millis)
{
    if (video_ptr) reinterpret_cast<VideoComposition::Data*>(video_ptr)->mVideo->seek(static_cast<float>(time_millis) / 1000.0f);
}

extern "C" void
Java_org_thorvg_core_video_VideoNativeBindings_nLoopVideo(JNIEnv* env, jclass clazz,
        jlong video_ptr, jboolean loop)
{
    if (video_ptr) reinterpret_cast<VideoComposition::Data*>(video_ptr)->mVideo->loop(loop == JNI_TRUE);
}

extern "C" void
Java_org_thorvg_core_video_VideoNativeBindings_nSetVideoVolume(JNIEnv* env, jclass clazz,
        jlong video_ptr, jfloat volume)
{
    if (video_ptr) reinterpret_cast<VideoComposition::Data*>(video_ptr)->mVideo->volume(volume);
}

extern "C" void
Java_org_thorvg_core_video_VideoNativeBindings_nSetVideoMuted(JNIEnv* env, jclass clazz,
        jlong video_ptr, jboolean muted)
{
    if (video_ptr) reinterpret_cast<VideoComposition::Data*>(video_ptr)->mVideo->mute(muted == JNI_TRUE);
}

extern "C" jlong
Java_org_thorvg_core_video_VideoNativeBindings_nGetVideoTime(JNIEnv* env, jclass clazz, jlong video_ptr)
{
    if (!video_ptr) return 0;
    return static_cast<jlong>(reinterpret_cast<VideoComposition::Data*>(video_ptr)->mVideo->time() * 1000.0f);
}

extern "C" void
Java_org_thorvg_core_video_VideoNativeBindings_nDestroyVideo(JNIEnv* env, jclass clazz, jlong video_ptr)
{
    if (!video_ptr) return;

    delete reinterpret_cast<VideoComposition::Data*>(video_ptr);
    tvg::Initializer::term();
}
