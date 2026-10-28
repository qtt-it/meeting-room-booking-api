package com.dki.meeting.foundation;

import java.util.List;

public record PageResult<T>(List<T> items, long totalItems, int page, int pageSize) {
  public static int offset(int page, int size) {
    if (page < 1 || page > 100000 || size < 1 || size > 100)
      throw new AppException(
          FailureKind.INVALID_REQUEST,
          "Validation.Pagination",
          "page must be 1-100000 and pageSize must be 1-100");
    return (page - 1) * size;
  }
}
