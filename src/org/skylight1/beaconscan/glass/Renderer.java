/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skylight1.beaconscan.glass;

import java.util.concurrent.TimeUnit;

import org.skylight1.beaconscan.R;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.glass.timeline.DirectRenderingCallback;
import com.google.android.glass.timeline.LiveCard;

/**
 * The surface callback that provides the rendering logic for the live card.
 */
public class Renderer implements DirectRenderingCallback {

    private static final String TAG = Renderer.class.getSimpleName();

    /** The refresh rate, in frames per second, of the compass. */
    private static final int REFRESH_RATE_FPS = 45;

    /** The duration, in milliseconds, of one frame. */
    private static final long FRAME_TIME_MILLIS = TimeUnit.SECONDS.toMillis(1) / REFRESH_RATE_FPS;

    private SurfaceHolder mHolder;
    private RenderThread mRenderThread;
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private boolean mRenderingPaused;

    private final FrameLayout mLayout;
    private final RelativeLayout mTipsContainer;
    private final TextView mTipsView;

    /**
     * Creates a new instance of the {@code Renderer}
     */
    public Renderer(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mLayout = (FrameLayout) inflater.inflate(R.layout.beaconscan_glass, null);
        mLayout.setWillNotDraw(false);

        mTipsContainer = (RelativeLayout) mLayout.findViewById(R.id.tips_container);
        mTipsView = (TextView) mLayout.findViewById(R.id.tips_view);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        doLayout();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The creation of a new Surface implicitly resumes the rendering.
        mRenderingPaused = false;
        mHolder = holder;
        updateRenderingState();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder = null;
        updateRenderingState();
    }

    @Override
    public void renderingPaused(SurfaceHolder holder, boolean paused) {
        mRenderingPaused = paused;
        updateRenderingState();
    }

    /**
     * Starts or stops rendering according to the {@link LiveCard}'s state.
     */
    private void updateRenderingState() {
        boolean shouldRender = (mHolder != null) && !mRenderingPaused;
        boolean isRendering = (mRenderThread != null);

        if (shouldRender != isRendering) {
            if (shouldRender) {
                mRenderThread = new RenderThread();
                mRenderThread.start();
            } else {
                mRenderThread.quit();
                mRenderThread = null;
            }
        }
    }

    /**
     * Requests that the views redo their layout. This must be called manually every time the
     * tips view's text is updated because this layout doesn't exist in a GUI thread where those
     * requests will be enqueued automatically.
     */
    private void doLayout() {
        // Measure and update the layout so that it will take up the entire surface space
        // when it is drawn.
        int measuredWidth = View.MeasureSpec.makeMeasureSpec(mSurfaceWidth,
                View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(mSurfaceHeight,
                View.MeasureSpec.EXACTLY);

        mLayout.measure(measuredWidth, measuredHeight);
        mLayout.layout(0, 0, mLayout.getMeasuredWidth(), mLayout.getMeasuredHeight());
    }

    /**
     * Repaints the compass.
     */
    private synchronized void repaint() {
        Canvas canvas = null;

        try {
            canvas = mHolder.lockCanvas();
        } catch (RuntimeException e) {
            Log.d(TAG, "lockCanvas failed", e);
        }

        if (canvas != null) {
            mLayout.draw(canvas);

            try {
                mHolder.unlockCanvasAndPost(canvas);
            } catch (RuntimeException e) {
                Log.d(TAG, "unlockCanvasAndPost failed", e);
            }
        }
    }

    /**
     * Shows or hides the tip view with an appropriate message based on the current accuracy of the
     * compass.
     */
    private void updateTipsView() {
        int stringId = 0;

        boolean show = (stringId != 0);

        if (show) {
            mTipsView.setText(stringId);
            doLayout();
        }

        if (mTipsContainer.getAnimation() == null) {
            float newAlpha = (show ? 1.0f : 0.0f);
            mTipsContainer.animate().alpha(newAlpha).start();
        }
    }

    /**
     * Redraws the compass in the background.
     */
    private class RenderThread extends Thread {
        private boolean mShouldRun;

        /**
         * Initializes the background rendering thread.
         */
        public RenderThread() {
            mShouldRun = true;
        }

        /**
         * Returns true if the rendering thread should continue to run.
         *
         * @return true if the rendering thread should continue to run
         */
        private synchronized boolean shouldRun() {
            return mShouldRun;
        }

        /**
         * Requests that the rendering thread exit at the next opportunity.
         */
        public synchronized void quit() {
            mShouldRun = false;
        }

        @Override
        public void run() {
            while (shouldRun()) {
                long frameStart = SystemClock.elapsedRealtime();
                repaint();
                long frameLength = SystemClock.elapsedRealtime() - frameStart;

                long sleepTime = FRAME_TIME_MILLIS - frameLength;
                if (sleepTime > 0) {
                    SystemClock.sleep(sleepTime);
                }
            }
        }
    }
	public View getViewById(int content) {
		return mLayout.findViewWithTag(content);
	}
}
