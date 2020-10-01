package io.harness.ng.core;

import com.google.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER)
@Singleton
public final class CorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter {
  @Override
  public void filter(ContainerRequestContext request) {
    String requestId = request.getHeaderString(CorrelationContext.getCorrelationIdKey());
    if (StringUtils.isEmpty(requestId)) {
      requestId = CorrelationContext.generateRandomUuid().toString();
    }
    CorrelationContext.setCorrelationId(requestId);
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    CorrelationContext.clearCorrelationId();
  }
}
