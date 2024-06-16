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

import static com.webfunny.rum.WebfunnyRum.COMPONENT_KEY;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.webkit.WebView;
import com.webfunny.rum.internal.GlobalAttributesSupplier;
import io.opentelemetry.android.OpenTelemetryRum;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WebfunnyRumTest {

    @RegisterExtension final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

    private Tracer tracer;

    @Mock private OpenTelemetryRum openTelemetryRum;
    @Mock private GlobalAttributesSupplier globalAttributes;

    @BeforeEach
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        WebfunnyRum.resetSingletonForTest();
    }

    @Test
    void initialization_onlyOnce() {
        Application application = mock(Application.class, RETURNS_DEEP_STUBS);
        Context context = mock(Context.class);

        WebfunnyRumBuilder webfunnyRumBuilder =
                new WebfunnyRumBuilder()
                        .setApplicationName("appName")
                        .setBeaconEndpoint("http://backend")
                        .setRumAccessToken("abracadabra")
                        .disableAnrDetection();

        when(application.getApplicationContext()).thenReturn(context);

        WebfunnyRum singleton = WebfunnyRum.initialize(webfunnyRumBuilder, application);
        WebfunnyRum sameInstance = webfunnyRumBuilder.build(application);

        assertSame(singleton, sameInstance);
    }

    @Test
    void getInstance_preConfig() {
        WebfunnyRum instance = WebfunnyRum.getInstance();
        assertTrue(instance instanceof NoOpWebfunnyRum);
    }

    @Test
    void getInstance() {
        Application application = mock(Application.class, RETURNS_DEEP_STUBS);
        Context context = mock(Context.class);

        WebfunnyRumBuilder webfunnyRumBuilder =
                new WebfunnyRumBuilder()
                        .setApplicationName("appName")
                        .setBeaconEndpoint("http://backend")
                        .setRumAccessToken("abracadabra")
                        .disableAnrDetection();

        when(application.getApplicationContext()).thenReturn(context);

        WebfunnyRum singleton = WebfunnyRum.initialize(webfunnyRumBuilder, application);
        assertSame(singleton, WebfunnyRum.getInstance());
    }

    @Test
    void newBuilder() {
        assertNotNull(WebfunnyRum.builder());
    }

    @Test
    void nonNullMethods() {
        Application application = mock(Application.class, RETURNS_DEEP_STUBS);
        Context context = mock(Context.class);

        when(application.getApplicationContext()).thenReturn(context);

        WebfunnyRumBuilder webfunnyRumBuilder =
                new WebfunnyRumBuilder()
                        .setApplicationName("appName")
                        .setBeaconEndpoint("http://backend")
                        .setRumAccessToken("abracadabra")
                        .disableAnrDetection();

        WebfunnyRum webfunnyRum = WebfunnyRum.initialize(webfunnyRumBuilder, application);
        assertNotNull(webfunnyRum.getOpenTelemetry());
        assertNotNull(webfunnyRum.getRumSessionId());
    }

    @Test
    void addEvent() {
        when(openTelemetryRum.getOpenTelemetry()).thenReturn(otelTesting.getOpenTelemetry());

        WebfunnyRum webfunnyRum = new WebfunnyRum(openTelemetryRum, globalAttributes);

        Attributes attributes = Attributes.of(stringKey("one"), "1", longKey("two"), 2L);
        webfunnyRum.addRumEvent("foo", attributes);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        assertEquals("foo", spans.get(0).getName());
        assertEquals(attributes.asMap(), spans.get(0).getAttributes().asMap());
    }

    @Test
    void addException() {
        InMemorySpanExporter testExporter = InMemorySpanExporter.create();
        OpenTelemetrySdk testSdk = buildTestSdk(testExporter);

        when(openTelemetryRum.getOpenTelemetry()).thenReturn(testSdk);

        WebfunnyRum webfunnyRum = new WebfunnyRum(openTelemetryRum, globalAttributes);

        NullPointerException exception = new NullPointerException("oopsie");
        Attributes attributes = Attributes.of(stringKey("one"), "1", longKey("two"), 2L);
        webfunnyRum.addRumException(exception, attributes);

        List<SpanData> spans = testExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());

        assertThat(spans.get(0))
                .hasName("NullPointerException")
                .hasAttributes(
                        attributes.toBuilder()
                                .put(COMPONENT_KEY, WebfunnyRum.COMPONENT_ERROR)
                                .build())
                .hasException(exception);
    }

    private OpenTelemetrySdk buildTestSdk(InMemorySpanExporter testExporter) {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(
                        SdkTracerProvider.builder()
                                .addSpanProcessor(SimpleSpanProcessor.create(testExporter))
                                .build())
                .build();
    }

    @Test
    void createAndEnd() {
        when(openTelemetryRum.getOpenTelemetry()).thenReturn(otelTesting.getOpenTelemetry());

        WebfunnyRum webfunnyRum = new WebfunnyRum(openTelemetryRum, globalAttributes);

        Span span = webfunnyRum.startWorkflow("workflow");
        Span inner = tracer.spanBuilder("foo").startSpan();
        try (Scope scope = inner.makeCurrent()) {
            // do nothing
        } finally {
            inner.end();
        }
        span.end();

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());
        // verify we're not trying to do any propagation of the context here.
        assertEquals(spans.get(0).getParentSpanId(), SpanId.getInvalid());
        assertEquals("foo", spans.get(0).getName());
        assertEquals("workflow", spans.get(1).getName());
        assertEquals("workflow", spans.get(1).getAttributes().get(WebfunnyRum.WORKFLOW_NAME_KEY));
    }

    @Test
    void integrateWithBrowserRum() {
        Application application = mock(Application.class, RETURNS_DEEP_STUBS);
        Context context = mock(Context.class);
        WebView webView = mock(WebView.class);

        when(application.getApplicationContext()).thenReturn(context);

        WebfunnyRumBuilder webfunnyRumBuilder =
                new WebfunnyRumBuilder()
                        .setApplicationName("appName")
                        .setBeaconEndpoint("http://backend")
                        .setRumAccessToken("abracadabra")
                        .disableAnrDetection();

        WebfunnyRum webfunnyRum = WebfunnyRum.initialize(webfunnyRumBuilder, application);
        webfunnyRum.integrateWithBrowserRum(webView);

        verify(webView)
                .addJavascriptInterface(isA(NativeRumSessionId.class), eq("WebfunnyRumNative"));
    }

    @Test
    void updateLocation() {
        AtomicReference<Attributes> updatedAttributes = new AtomicReference<>();
        GlobalAttributesSupplier globalAttributes = mock(GlobalAttributesSupplier.class);
        doAnswer(
                        invocation -> {
                            Consumer<AttributesBuilder> updater = invocation.getArgument(0);

                            AttributesBuilder attributesBuilder = Attributes.builder();
                            updater.accept(attributesBuilder);
                            updatedAttributes.set(attributesBuilder.build());
                            return null;
                        })
                .when(globalAttributes)
                .update(isA(Consumer.class));

        WebfunnyRum webfunnyRum = new WebfunnyRum(openTelemetryRum, globalAttributes);

        Location location = mock(Location.class);
        when(location.getLatitude()).thenReturn(42d);
        when(location.getLongitude()).thenReturn(43d);
        webfunnyRum.updateLocation(location);

        assertEquals(
                Attributes.of(
                        WebfunnyRum.LOCATION_LATITUDE_KEY,
                        42d,
                        WebfunnyRum.LOCATION_LONGITUDE_KEY,
                        43d),
                updatedAttributes.get());

        webfunnyRum.updateLocation(null);

        assertTrue(updatedAttributes.get().isEmpty());
    }
}
