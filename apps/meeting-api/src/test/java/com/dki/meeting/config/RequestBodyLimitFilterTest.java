package com.dki.meeting.config;

import static org.assertj.core.api.Assertions.*;

import com.dki.meeting.foundation.http.RequestIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

class RequestBodyLimitFilterTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final RequestBodyLimitFilter filter = new RequestBodyLimitFilter(json);

  @Test
  void rejectsDeclaredOversizeWithoutCallingTheController() throws Exception {
    var request = request(RequestBodyLimitFilter.MAX_BYTES + 1, false);
    var response = new MockHttpServletResponse();
    var called = new AtomicBoolean();
    new RequestIdFilter()
        .doFilter(
            request, response, (req, res) -> filter.doFilter(req, res, (q, r) -> called.set(true)));
    assertThat(called).isFalse();
    assertThat(response.getStatus()).isEqualTo(413);
    var error = json.readTree(response.getContentAsString());
    assertThat(error.path("error").path("code").asText()).isEqualTo("Request.PayloadTooLarge");
    assertThat(error.path("meta").path("requestId").asText())
        .isEqualTo(response.getHeader("X-Request-Id"));
    assertThat(response.getContentAsString()).doesNotContain("aaa", "Exception");
  }

  @Test
  void rejectsUnknownLengthOversizeIncludingChunkedBodies() throws Exception {
    var response = new MockHttpServletResponse();
    var called = new AtomicBoolean();
    filter.doFilter(
        request(RequestBodyLimitFilter.MAX_BYTES + 1, true), response, (q, r) -> called.set(true));
    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(called).isFalse();
  }

  @Test
  void permitsExactlyTheLimitAndPreservesTheBody() throws Exception {
    var request = request(RequestBodyLimitFilter.MAX_BYTES, true);
    var response = new MockHttpServletResponse();
    filter.doFilter(
        request,
        response,
        (q, r) ->
            assertThat(q.getInputStream().readAllBytes())
                .isEqualTo(request.getContentAsByteArray()));
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void doesNotInterceptNonApiRequests() throws Exception {
    var request = request(RequestBodyLimitFilter.MAX_BYTES + 1, false);
    request.setRequestURI("/actuator/health");
    var response = new MockHttpServletResponse();
    var called = new AtomicBoolean();
    filter.doFilter(request, response, (q, r) -> called.set(true));
    assertThat(called).isTrue();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  private MockHttpServletRequest request(int size, boolean unknownLength) {
    var request =
        new MockHttpServletRequest("POST", "/api/v1/auth/login") {
          @Override
          public long getContentLengthLong() {
            return unknownLength ? -1 : super.getContentLengthLong();
          }
        };
    request.setContentType("application/json");
    request.setContent("a".repeat(size).getBytes(StandardCharsets.UTF_8));
    return request;
  }
}
