package com.dki.meeting.foundation.http;

import com.dki.meeting.foundation.PageResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, Failure error, Meta meta) {
  public record Failure(String code, String message, Object details) {}

  public record Pagination(int page, int pageSize, long totalItems, long totalPages) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Meta(String requestId, Instant timestamp, Pagination pagination) {}

  public static <T> ApiResponse<T> ok(T data, String message) {
    return new ApiResponse<>(true, message, data, null, meta(null));
  }

  public static <T> ApiResponse<List<T>> page(PageResult<T> page) {
    return new ApiResponse<>(
        true,
        "Retrieved successfully",
        page.items(),
        null,
        meta(
            new Pagination(
                page.page(),
                page.pageSize(),
                page.totalItems(),
                (page.totalItems() + page.pageSize() - 1) / page.pageSize())));
  }

  public static ApiResponse<Void> failure(String code, String message, Object details) {
    return new ApiResponse<>(false, message, null, new Failure(code, message, details), meta(null));
  }

  private static Meta meta(Pagination pagination) {
    return new Meta(MDC.get("requestId"), Instant.now(), pagination);
  }
}
