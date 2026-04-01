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

#include <thread>
#include <android/log.h>
#include "LottieData.h"

LottieDrawable::Data::Data(const char *content, uint32_t length) {
    LOGI("LottieDrawable::Data::Data length=%d", length);
    mContent = content;
    mContentLength = length;

    // Generate an animation
    mAnimation = tvg::Animation::gen();
    // Acquire a picture which associated with the animation.
    auto picture = mAnimation->picture();
    if (picture->load(mContent, mContentLength, "lottie", true) != tvg::Result::Success) {
        LOGE("Error: Lottie is not supported. Did you enable Lottie Loader?");
        return;
    }

    // Create a canvas
    mCanvas = tvg::SwCanvas::gen();
    mCanvas->push(tvg::cast<tvg::Picture>(picture));
}

void LottieDrawable::Data::setBufferSize(uint32_t *buffer, float width, float height) {
    LOGI("LottieDrawable::Data::setBufferSize width=%f, height=%f", width, height);
    mCanvas->sync();
    mCanvas->clear(false);
    mCanvas->target(buffer, (uint32_t) width, (uint32_t) width, (uint32_t) height,
            tvg::SwCanvas::ABGR8888);
    mAnimation->picture()->size(width, height);
}

void LottieDrawable::Data::draw(uint32_t frame) {
    if (!mCanvas) return;
//    LOGI("LottieDrawable::Data::draw mAnimation=%d", mAnimation->curFrame());
    mAnimation->frame(frame);
    mCanvas->update(mAnimation->picture());
    mCanvas->draw();
    mCanvas->sync();
}
