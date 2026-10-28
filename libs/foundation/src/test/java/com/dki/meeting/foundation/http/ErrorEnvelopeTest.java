package com.dki.meeting.foundation.http;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dki.meeting.foundation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

class ErrorEnvelopeTest {
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc =
        MockMvcBuilders.standaloneSetup(new Endpoints())
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new RequestIdFilter())
            .build();
  }

  @Test
  void domainFailureUsesStableErrorContract() throws Exception {
    mvc.perform(get("/domain").header("X-Request-Id", "contract-test"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("BusinessRule.Violated"))
        .andExpect(jsonPath("$.meta.requestId").value("contract-test"));
  }

  @Test
  void unexpectedFailureDoesNotExposeInternalDetails() throws Exception {
    var result =
        mvc.perform(get("/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error.code").value("Internal.Unexpected"))
            .andReturn();
    assertThat(result.getResponse().getContentAsString())
        .doesNotContain("private-database-detail", "stackTrace", "SQLException");
  }

  @Test
  void invalidBodyReturnsValidationDetails() throws Exception {
    mvc.perform(
            post("/validated").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.details[0].target").value("name"));
  }

  @Test
  void paginationRejectsInvalidAndOversizedRequests() {
    assertThatThrownBy(() -> PageResult.offset(0, 20)).isInstanceOf(AppException.class);
    assertThatThrownBy(() -> PageResult.offset(1, 101)).isInstanceOf(AppException.class);
    assertThat(PageResult.offset(2, 20)).isEqualTo(20);
  }

  @RestController
  static class Endpoints {
    record Input(@NotBlank String name) {}

    @GetMapping("/domain")
    void domain() {
      throw AppException.invalid("This action is not allowed");
    }

    @GetMapping("/unexpected")
    void unexpected() {
      throw new IllegalStateException("private-database-detail");
    }

    @PostMapping("/validated")
    ApiResponse<?> validated(@Valid @RequestBody Input input) {
      return ApiResponse.ok(input, "Accepted");
    }
  }
}
