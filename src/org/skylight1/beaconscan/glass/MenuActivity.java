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

import org.skylight1.beaconscan.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;

/**
 * This activity manages the options menu that appears when the user taps on the compass's live
 * card.
 */
public class MenuActivity extends Activity {

    private final Handler mHandler = new Handler();

    private GlassService.CompassBinder mCompassService;
    private boolean mAttachedToWindow;
    private boolean mOptionsMenuOpen;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof GlassService.CompassBinder) {
                mCompassService = (GlassService.CompassBinder) service;
                openOptionsMenu();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Do nothing.
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService(new Intent(this, GlassService.class), mConnection, 0);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;
        openOptionsMenu();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;
    }

    @Override
    public void openOptionsMenu() {
        if (!mOptionsMenuOpen && mAttachedToWindow && mCompassService != null) {
            super.openOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.beaconscan, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.read_aloud:
                mCompassService.readHeadingAloud();
                return true;
            case R.id.stop:
                // Stop the service at the end of the message queue for proper options menu
                // animation. This is only needed when starting an Activity or stopping a Service
                // that published a LiveCard.
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        stopService(new Intent(MenuActivity.this, GlassService.class));
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        mOptionsMenuOpen = false;

        unbindService(mConnection);

        // We must call finish() from this method to ensure that the activity ends either when an
        // item is selected from the menu or when the menu is dismissed by swiping down.
        finish();
    }
}
