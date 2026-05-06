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

#include "SvgData.h"

SvgComposition::Data::Data(const char *content, uint32_t length) {
    SVG_LOGI("SvgComposition::Data::Data length=%d", length);

    auto picture = tvg::Picture::gen();
    if (picture->load(content, length, "svg", nullptr, true) != tvg::Result::Success) {
        SVG_LOGE("Error: SVG is not supported. Did you enable SVG Loader?");
        tvg::Paint::rel(picture);
        return;
    }

    picture->size(&mWidth, &mHeight);

    mCanvas = tvg::SwCanvas::gen(tvg::EngineOption::Default);
    mPicture = picture;
    mCanvas->add(mPicture);
}

SvgComposition::Data::~Data() {
    delete(mCanvas);
}

void SvgComposition::Data::setBufferSize(uint32_t *buffer, float width, float height) {
    if (!mCanvas || !mPicture) return;

    SVG_LOGI("SvgComposition::Data::setBufferSize width=%f, height=%f", width, height);
    mCanvas->sync();
    mCanvas->target(buffer, (uint32_t) width, (uint32_t) width, (uint32_t) height,
                    tvg::ColorSpace::ABGR8888);
    mPicture->size(width, height);
}

void SvgComposition::Data::draw() {
    if (!mCanvas || !mPicture) return;

    mCanvas->update();
    mCanvas->draw(true);
    mCanvas->sync();
}
