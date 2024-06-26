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

package com.webfunny.rum;

import static java.util.Objects.requireNonNull;

import android.util.Log;
import androidx.annotation.Nullable;
import io.opentelemetry.sdk.common.Clock;
import java.io.File;
import java.io.IOException;
import java.util.List;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;

class ZipkinToDiskSender implements BytesMessageSender {

    private final SpanStorage spanStorage;
    private final FileUtils fileUtils;
    private final Clock clock;
    private final DeviceSpanStorageLimiter storageLimiter;

    private ZipkinToDiskSender(Builder builder) {
        this.spanStorage = requireNonNull(builder.fileProvider);
        this.fileUtils = builder.fileUtils;
        this.clock = builder.clock;
        this.storageLimiter = requireNonNull(builder.storageLimiter);
    }

    @Override
    public Encoding encoding() {
        return Encoding.JSON;
    }

    @Override
    public int messageMaxBytes() {
        return 1024 * 1024;
    }

    @Override
    public int messageSizeInBytes(List<byte[]> encodedSpans) {
        return encodedSpans.stream().reduce(0, (acc, cur) -> acc + cur.length + 1, Integer::sum);
    }

    @Override
    public void send(List<byte[]> encodedSpans) throws IOException {
        if (encodedSpans.isEmpty()) {
            return;
        }
        if (!storageLimiter.ensureFreeSpace()) {
            Log.e(
                    WebfunnyRum.LOG_TAG,
                    "Dropping "
                            + encodedSpans.size()
                            + " spans: Too much telemetry has been buffered or not enough space on device.");
            return;
        }
        long now = clock.now();
        File filename = createFilename(now);
        try {
            fileUtils.writeAsLines(filename, encodedSpans);
        } catch (IOException e) {
            Log.e(WebfunnyRum.LOG_TAG, "Error writing spans to storage", e);
        }
    }

    @Override
    public int messageSizeInBytes(int encodedSizeInBytes) {
        return encoding().listSizeInBytes(encodedSizeInBytes);
    }

    private File createFilename(long now) {
        return new File(spanStorage.provideSpansDirectory(), now + ".spans");
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    public void close() throws IOException {}

    static class Builder {
        @Nullable private SpanStorage fileProvider;
        private FileUtils fileUtils = new FileUtils();
        private Clock clock = Clock.getDefault();
        @Nullable private DeviceSpanStorageLimiter storageLimiter;

        Builder spanFileProvider(SpanStorage spanStorage) {
            this.fileProvider = spanStorage;
            return this;
        }

        Builder fileUtils(FileUtils fileUtils) {
            this.fileUtils = fileUtils;
            return this;
        }

        Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        Builder storageLimiter(DeviceSpanStorageLimiter limiter) {
            this.storageLimiter = limiter;
            return this;
        }

        ZipkinToDiskSender build() {
            return new ZipkinToDiskSender(this);
        }
    }
}
