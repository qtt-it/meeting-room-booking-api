package com.dki.meeting.foundation.http;

import com.dki.meeting.foundation.OperationContext;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.*;

public final class OperationContextResolver implements HandlerMethodArgumentResolver {
  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentOperation.class)
        && parameter.getParameterType().equals(OperationContext.class);
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer container,
      NativeWebRequest request,
      WebDataBinderFactory factory) {
    return RequestContext.current();
  }
}
