package com.dki.meeting.foundation.http;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.*;

class RequestIdFilterTest {
  @Test
  void rejectsUnsafeRequestIdsAndClearsContext() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Request-Id", "bad\r\nheader");
    var response = new MockHttpServletResponse();
    new RequestIdFilter()
        .doFilter(
            request,
            response,
            (req, res) -> assertThat(MDC.get("requestId")).matches("[a-f0-9-]{36}"));
    assertThat(response.getHeader("X-Request-Id")).matches("[a-f0-9-]{36}");
    assertThat(MDC.get("requestId")).isNull();
  }

  @Test
  void propagatesValidRequestId() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Request-Id", "test-request-123");
    var response = new MockHttpServletResponse();
    new RequestIdFilter()
        .doFilter(
            request,
            response,
            (req, res) -> assertThat(MDC.get("requestId")).isEqualTo("test-request-123"));
    assertThat(response.getHeader("X-Request-Id")).isEqualTo("test-request-123");
  }
}
