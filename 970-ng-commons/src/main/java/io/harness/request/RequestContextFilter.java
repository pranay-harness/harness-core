/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.request;

import static io.harness.request.RequestContextData.REQUEST_CONTEXT;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.context.GlobalContext;
import io.harness.manage.GlobalContextManager;
import io.harness.request.RequestContext.RequestContextBuilder;

import software.wings.beans.HttpMethod;

import com.google.inject.Singleton;
import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;

@Singleton
@Provider
@Priority(1500)
@Slf4j
public class RequestContextFilter implements ContainerRequestFilter, ContainerResponseFilter {
  private static final String X_FORWARDED_FOR = "X-Forwarded-For";

  @Context private ResourceContext resourceContext;
  @Context private ResourceInfo resourceInfo;
  @Context private HttpServletRequest servletRequest;

  @Override
  public void filter(ContainerRequestContext containerRequestContext) {
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }

    RequestContextBuilder requestContextBuilder = RequestContext.builder();

    HttpMethod method = HttpMethod.valueOf(containerRequestContext.getMethod());
    HttpRequestInfo httpRequestInfo = HttpRequestInfo.builder().requestMethod(method.name()).build();
    requestContextBuilder.httpRequestInfo(httpRequestInfo);

    RequestMetadata requestMetadata = RequestMetadata.builder().clientIP(getClientIP()).build();
    requestContextBuilder.requestMetadata(requestMetadata);

    GlobalContextManager.upsertGlobalContextRecord(
        RequestContextData.builder().requestContext(requestContextBuilder.build()).build());
  }

  private String getClientIP() {
    String forwardedFor = servletRequest.getHeader(X_FORWARDED_FOR);
    String remoteAddr = servletRequest.getRemoteAddr();
    if (isNotBlank(forwardedFor)) {
      return forwardedFor;
    } else if (isNotBlank(remoteAddr)) {
      return remoteAddr;
    }
    return servletRequest.getRemoteHost();
  }

  @Override
  public void filter(
      ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
    GlobalContextManager.unset();
  }

  @NotNull
  public static Interceptor getRequestContextInterceptor() {
    return chain -> {
      Request request = chain.request();
      RequestContextData requestContextData = GlobalContextManager.get(REQUEST_CONTEXT);
      if (requestContextData != null && requestContextData.getRequestContext() != null
          && requestContextData.getRequestContext().getRequestMetadata() != null
          && isNotBlank(requestContextData.getRequestContext().getRequestMetadata().getClientIP())) {
        return chain.proceed(
            request.newBuilder()
                .header(X_FORWARDED_FOR, requestContextData.getRequestContext().getRequestMetadata().getClientIP())
                .build());
      } else {
        return chain.proceed(request);
      }
    };
  }
}
