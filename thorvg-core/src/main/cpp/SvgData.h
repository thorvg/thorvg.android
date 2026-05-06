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

#ifndef THORVG_ANDROID_SVGDATA_H
#define THORVG_ANDROID_SVGDATA_H

#include <cstdint>
#include <android/log.h>
#include "thorvg.h"

#define SVG_LOG_TAG "SvgComposition"

#define SVG_LOGI(...) __android_log_print(ANDROID_LOG_INFO, SVG_LOG_TAG, __VA_ARGS__)
#define SVG_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, SVG_LOG_TAG, __VA_ARGS__)

namespace SvgComposition {

    class Data {
    public:
        Data(const char* content, uint32_t strLength);
        ~Data();
        void setBufferSize(uint32_t* buffer, float width, float height);
        void draw();
        tvg::Picture* mPicture = nullptr;
        float mWidth = 0.0f;
        float mHeight = 0.0f;
    private:
        tvg::SwCanvas* mCanvas = nullptr;
    };

} // namespace SvgComposition

#endif //THORVG_ANDROID_SVGDATA_H
