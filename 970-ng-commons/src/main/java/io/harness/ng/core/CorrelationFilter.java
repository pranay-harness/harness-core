/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(PL)
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
