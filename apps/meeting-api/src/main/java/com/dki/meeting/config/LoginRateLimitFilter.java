package com.dki.meeting.config;

import com.dki.meeting.foundation.http.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(-100)
public class LoginRateLimitFilter extends OncePerRequestFilter {
  private record Window(long expiresAt, int attempts) {}

  private final ConcurrentHashMap<String, Window> clients = new ConcurrentHashMap<>();
  private final AtomicLong calls = new AtomicLong();
  private final ObjectMapper json;

  public LoginRateLimitFilter(ObjectMapper json) {
    this.json = json;
  }

  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!request.getRequestURI().equals("/api/v1/auth/login")
        || !request.getMethod().equals("POST")) {
      chain.doFilter(request, response);
      return;
    }
    long now = System.currentTimeMillis();
    if (calls.incrementAndGet() % 100 == 0)
      clients.entrySet().removeIf(e -> e.getValue().expiresAt() < now);
    String key = request.getRemoteAddr();
    if (clients.size() >= 10000 && !clients.containsKey(key)) {
      reject(response);
      return;
    }
    var state =
        clients.compute(
            key,
            (k, v) ->
                v == null || v.expiresAt() < now
                    ? new Window(now + 60000, 1)
                    : new Window(v.expiresAt(), v.attempts() + 1));
    if (state.attempts() > 20) {
      reject(response);
      return;
    }
    chain.doFilter(request, response);
  }

  private void reject(HttpServletResponse response) throws IOException {
    response.setStatus(429);
    response.setHeader("Retry-After", "60");
    response.setContentType("application/json");
    json.writeValue(
        response.getOutputStream(),
        ApiResponse.failure(
            "Authentication.RateLimited", "Too many sign-in attempts. Retry later", null));
  }
}
