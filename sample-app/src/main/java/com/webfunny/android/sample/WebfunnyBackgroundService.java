/*
 * Copyright Webfunny Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webfunny.android.sample;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class WebfunnyBackgroundService extends Service {
    public static final String TAG = "WebfunnyRum";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Log.d(TAG, "Service started on different thread");
        }
        stopSelf();
        return START_STICKY;
    }
}
