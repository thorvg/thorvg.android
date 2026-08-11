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

#include "VideoData.h"

VideoComposition::Data::Data(const char* path)
{
    if (!path) return;

    mVideo = tvg::Video::gen();
    if (!mVideo) return;

    auto picture = mVideo->picture();
    if (!picture) {
        delete mVideo;
        mVideo = nullptr;
        return;
    }

    auto ret = picture->load(path);
    if (ret != tvg::Result::Success) {
        delete mVideo;
        mVideo = nullptr;
        return;
    }

    picture->size(&mWidth, &mHeight);
    mDuration = mVideo->duration();

    mCanvas = tvg::SwCanvas::gen(tvg::EngineOption::Default);
    if (!mCanvas || mCanvas->add(picture) != tvg::Result::Success) {
        delete mCanvas;
        delete mVideo;
        mCanvas = nullptr;
        mVideo = nullptr;
        return;
    }
}

VideoComposition::Data::~Data()
{
    delete mCanvas;
    delete mVideo;
}

bool VideoComposition::Data::valid()
{
    return mVideo && mCanvas;
}

void VideoComposition::Data::setBufferSize(uint32_t* buffer, float width, float height)
{
    if (!valid()) return;

    mBufferWidth = width;
    mBufferHeight = height;
    mCanvas->sync();
    mCanvas->target(buffer, static_cast<uint32_t>(width), static_cast<uint32_t>(width), static_cast<uint32_t>(height), tvg::ColorSpace::ABGR8888);
    mVideo->picture()->size(width, height);
}

void VideoComposition::Data::draw(uint32_t* buffer)
{
    if (!valid()) return;

    if (buffer && mBufferWidth > 0.0f && mBufferHeight > 0.0f) {
        mCanvas->target(buffer, static_cast<uint32_t>(mBufferWidth), static_cast<uint32_t>(mBufferWidth), static_cast<uint32_t>(mBufferHeight), tvg::ColorSpace::ABGR8888);
    }
    mCanvas->update();
    mCanvas->draw(true);
    mCanvas->sync();
}
