/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
