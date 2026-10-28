package com.dki.meeting;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BoundaryIT extends ApiTestSupport {
  @Test
  void openApiDoesNotExposeInternalOperationContext() throws Exception {
    var document = call("GET", "/v3/api-docs", null, null, 200);
    assertThat(document.path("components").path("schemas").has("OperationContext")).isFalse();
    for (var path : document.path("paths"))
      for (var operation : path) {
        for (var parameter : operation.path("parameters"))
          assertThat(parameter.path("name").asText()).isNotIn("context", "actor", "requestId");
      }
  }
}
