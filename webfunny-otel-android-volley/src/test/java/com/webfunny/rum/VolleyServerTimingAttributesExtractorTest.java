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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.volley.Header;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpResponse;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class VolleyServerTimingAttributesExtractorTest {

    static final String TRACE_ID = "9499195c502eb217c448a68bfe0f967c";
    static final String SPAN_ID = "fe16eca542cd5d86";

    @Test
    public void serverTiming() {
        ServerTimingHeaderParser headerParser = mock(ServerTimingHeaderParser.class);
        when(headerParser.parse("headerValue")).thenReturn(new String[] {TRACE_ID, SPAN_ID});

        List<Header> responseHeaders =
                Collections.singletonList(new Header("Server-Timing", "headerValue"));
        RequestWrapper fakeRequest =
                new RequestWrapper(mock(Request.class), Collections.emptyMap());
        HttpResponse response = new HttpResponse(200, responseHeaders, "hello".getBytes());

        VolleyServerTimingAttributesExtractor attributesExtractor =
                new VolleyServerTimingAttributesExtractor(headerParser);
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesExtractor.onStart(attributesBuilder, null, fakeRequest);
        attributesExtractor.onEnd(attributesBuilder, null, fakeRequest, response, null);
        Attributes attributes = attributesBuilder.build();

        assertEquals(TRACE_ID, attributes.get(WebfunnyRum.LINK_TRACE_ID_KEY));
        assertEquals(SPAN_ID, attributes.get(WebfunnyRum.LINK_SPAN_ID_KEY));
    }

    @Test
    public void spanDecoration_noLinkingHeader() {
        ServerTimingHeaderParser headerParser = mock(ServerTimingHeaderParser.class);
        when(headerParser.parse(null)).thenReturn(new String[0]);

        RequestWrapper fakeRequest =
                new RequestWrapper(mock(Request.class), Collections.emptyMap());
        HttpResponse response = new HttpResponse(200, Collections.emptyList(), "hello".getBytes());

        VolleyServerTimingAttributesExtractor attributesExtractor =
                new VolleyServerTimingAttributesExtractor(headerParser);
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesExtractor.onEnd(attributesBuilder, null, fakeRequest, response, null);
        attributesExtractor.onStart(attributesBuilder, null, fakeRequest);
        Attributes attributes = attributesBuilder.build();

        assertNull(attributes.get(WebfunnyRum.LINK_TRACE_ID_KEY));
        assertNull(attributes.get(WebfunnyRum.LINK_SPAN_ID_KEY));
    }
}
