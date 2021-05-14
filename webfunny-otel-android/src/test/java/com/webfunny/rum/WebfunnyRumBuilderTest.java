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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import android.app.Application;
import io.opentelemetry.api.common.Attributes;
import org.junit.jupiter.api.Test;

class WebfunnyRumBuilderTest {

    @Test
    void buildingRequiredFields() {
        Application app = mock(Application.class);

        assertThrows(IllegalStateException.class, () -> WebfunnyRum.builder().build(app));
        assertThrows(
                IllegalStateException.class,
                () ->
                        WebfunnyRum.builder()
                                .setRumAccessToken("abc123")
                                .setBeaconEndpoint("http://backend")
                                .build(app));
        assertThrows(
                IllegalStateException.class,
                () ->
                        WebfunnyRum.builder()
                                .setBeaconEndpoint("http://backend")
                                .setApplicationName("appName")
                                .build(app));
        assertThrows(
                IllegalStateException.class,
                () ->
                        WebfunnyRum.builder()
                                .setApplicationName("appName")
                                .setRumAccessToken("abc123")
                                .build(app));
    }

    @Test
    void defaultValues() {
        WebfunnyRumBuilder builder = WebfunnyRum.builder();

        assertFalse(builder.isDebugEnabled());
        assertFalse(builder.isDiskBufferingEnabled());
        assertTrue(builder.isCrashReportingEnabled());
        assertTrue(builder.isNetworkMonitorEnabled());
        assertTrue(builder.isAnrDetectionEnabled());
        assertTrue(builder.isSlowRenderingDetectionEnabled());
        assertEquals(Attributes.empty(), builder.globalAttributes);
        assertNull(builder.deploymentEnvironment);
        assertFalse(builder.sessionBasedSamplerEnabled);
    }

    @Test
    void handleNullAttributes() {
        WebfunnyRumBuilder builder = WebfunnyRum.builder().setGlobalAttributes(null);
        assertEquals(Attributes.empty(), builder.globalAttributes);
    }

    @Test
    void setBeaconFromRealm() {
        WebfunnyRumBuilder builder = WebfunnyRum.builder().setRealm("us0");
        assertEquals("https://rum-ingest.us0.signalfx.com/v1/rum", builder.beaconEndpoint);
    }

    @Test
    void beaconOverridesRealm() {
        WebfunnyRumBuilder builder =
                WebfunnyRum.builder().setRealm("us0").setBeaconEndpoint("http://beacon");
        assertEquals("http://beacon", builder.beaconEndpoint);
    }

    @Test
    void otlpNotEnabledByDefault() {
        WebfunnyRumBuilder builder = WebfunnyRum.builder().setRealm("jp0");
        assertThat(builder.getConfigFlags().shouldUseOtlpExporter()).isFalse();
    }

    @Test
    void enableOtlp() {
        WebfunnyRumBuilder builder =
                WebfunnyRum.builder().setRealm("jp0").enableExperimentalOtlpExporter();
        assertThat(builder.getConfigFlags().shouldUseOtlpExporter()).isTrue();
    }

    @Test
    void otlpFailsWhenDiskBufferingEnabled() {
        WebfunnyRumBuilder builder =
                WebfunnyRum.builder()
                        .setRealm("us0")
                        .enableDiskBuffering()
                        .enableExperimentalOtlpExporter();
        assertThat(builder.getConfigFlags().shouldUseOtlpExporter()).isFalse();
    }

    @Test
    void enableDiskBufferAfterOtlp() {
        WebfunnyRumBuilder builder =
                WebfunnyRum.builder()
                        .setRealm("us0")
                        .enableExperimentalOtlpExporter()
                        .enableDiskBuffering();
        assertThat(builder.getConfigFlags().shouldUseOtlpExporter()).isFalse();
    }
}
