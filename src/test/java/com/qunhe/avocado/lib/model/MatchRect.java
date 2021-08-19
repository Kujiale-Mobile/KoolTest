/*
  Copyright [2021] [Manycore Tech Inc.]

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.qunhe.avocado.lib.model;

import lombok.ToString;

@ToString
public class MatchRect {
    public static class Point {
        public int X;
        public int Y;

        public Point(int x, int y) {
            X = x;
            Y = y;
        }
    }

    private final int mStartX;
    private final int mStartY;
    private final int mEndX;
    private final int mEndY;

    public MatchRect(int startX, int startY, int endX, int endY) {
        this.mStartX = startX;
        this.mStartY = startY;
        this.mEndX = endX;
        this.mEndY = endY;
    }

    public int getStartX() {
        return this.mStartX;
    }

    public int getStartY() {
        return this.mStartY;
    }

    public int getWidth() {
        return this.mEndX - this.mStartX;
    }

    public int getHeight() {
        return this.mEndY - this.mStartY;
    }

    public Point center() {
        return new Point((mStartX + mEndX) / 2, (mStartY + mEndY) / 2);
    }
}
