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

#include <android/log.h>
#include "LottieData.h"

LottieDrawable::Data::Data(JNIEnv* env, jstring content) {
    if (content == nullptr) return;

    const char* inputStr = env->GetStringUTFChars(content, nullptr);
    if (inputStr == nullptr) return;

    auto length = static_cast<uint32_t>(env->GetStringUTFLength(content));
    LOGI("LottieDrawable::Data::Data length=%d", length);

    mAnimation = tvg::Animation::gen();
    auto picture = mAnimation->picture();
    if (picture->load(inputStr, length, "lottie", nullptr, true) != tvg::Result::Success) {
        LOGE("Error: Lottie is not supported. Did you enable Lottie Loader?");
        env->ReleaseStringUTFChars(content, inputStr);
        return;
    }
    env->ReleaseStringUTFChars(content, inputStr);
    mValid = true;
}

LottieDrawable::Data::~Data() {
    delete(mCanvas);
    delete(mAnimation);
}

bool LottieDrawable::Data::valid() {
    return mValid && mCanvas != nullptr;
}

void LottieDrawable::Data::draw(uint32_t frame) {
//    LOGI("LottieDrawable::Data::draw mAnimation=%d", mAnimation->curFrame());
    mAnimation->frame(frame);
    mCanvas->update();
    mCanvas->draw(true);
    mCanvas->sync();
}

LottieDrawable::SwData::SwData(JNIEnv* env, jstring content) : Data(env, content) {
    if (!mValid) return;

    mCanvas = tvg::SwCanvas::gen(tvg::EngineOption::Default);
    mCanvas->add(mAnimation->picture());
}

bool LottieDrawable::SwData::resize(uint32_t *buffer, float width, float height) {
    LOGI("LottieDrawable::SwData::resize width=%f, height=%f", width, height);
    mCanvas->sync();
    auto result = static_cast<tvg::SwCanvas*>(mCanvas)->target(buffer, (uint32_t) width, (uint32_t) width, (uint32_t) height, tvg::ColorSpace::ABGR8888);
    mAnimation->picture()->size(width, height);
    return result == tvg::Result::Success;
}

LottieDrawable::GlData::GlData(JNIEnv* env, jstring content) : Data(env, content) {
    if (!mValid) return;

    mCanvas = tvg::GlCanvas::gen(tvg::EngineOption::Default);
    mCanvas->add(mAnimation->picture());
}

bool LottieDrawable::GlData::resize(void* display, void* surface, void* context, int32_t id, float width, float height) {
    LOGI("LottieDrawable::GlData::resize width=%f, height=%f, fbo=%d", width, height, id);
    mCanvas->sync();
    auto result = static_cast<tvg::GlCanvas*>(mCanvas)->target(display, surface, context, id, (uint32_t) width, (uint32_t) height, tvg::ColorSpace::ABGR8888S);
    mAnimation->picture()->size(width, height);
    return result == tvg::Result::Success;
}
