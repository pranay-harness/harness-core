package io.harness.request;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.context.GlobalContext;
import io.harness.manage.GlobalContextManager;
import io.harness.request.RequestContext.RequestContextBuilder;

import software.wings.beans.HttpMethod;

import com.google.inject.Singleton;
import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

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
    requestContextBuilder.requestMethod(method.name());

    requestContextBuilder.clientIP(getClientIP());

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
}
