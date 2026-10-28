package com.dki.meeting.foundation.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class JsonSecurityErrors {
  private final ObjectMapper json;

  public JsonSecurityErrors(ObjectMapper json) {
    this.json = json;
  }

  public void write(HttpServletResponse response, int status) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    String code = status == 401 ? "Authentication.Required" : "Authorization.Forbidden";
    String message =
        status == 401 ? "Authentication is required" : "You cannot perform this action";
    json.writeValue(response.getOutputStream(), ApiResponse.failure(code, message, null));
  }
}
