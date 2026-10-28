package com.dki.meeting.foundation.http;

import com.dki.meeting.foundation.*;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;

public final class RequestContext {
  private RequestContext() {}

  public static OperationContext current() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    Actor actor =
        authentication != null && authentication.getPrincipal() instanceof Actor a ? a : null;
    return new OperationContext(actor, MDC.get("requestId"));
  }
}
