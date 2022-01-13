/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.auth;

import io.harness.grpc.InterceptorPriority;
import io.harness.grpc.utils.GrpcAuthUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * ensures that the request is validated by some auth interceptors
 */
@Slf4j
@Singleton
@InterceptorPriority(Integer.MAX_VALUE)
public class ValidateAuthServerInterceptor implements ServerInterceptor {
  private static final Listener NOOP_LISTENER = new Listener() {};
  private final Set<String> excludedGrpcServices;

  @Inject
  public ValidateAuthServerInterceptor(@Named("excludedGrpcAuthValidationServices") Set<String> excludedGrpcServices) {
    this.excludedGrpcServices = excludedGrpcServices;
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> next) {
    if (GrpcAuthUtils.isAuthenticated() || excluded(call)) {
      return Contexts.interceptCall(Context.current(), call, metadata, next);
    }
    log.warn("No Auth interceptor could validate this request. Please add one for this service");
    call.close(Status.UNAUTHENTICATED.withDescription("Unable to authenticate request"), metadata);
    return NOOP_LISTENER;
  }

  private <RespT, ReqT> boolean excluded(ServerCall<ReqT, RespT> call) {
    return excludedGrpcServices.contains(call.getMethodDescriptor().getServiceName());
  }
}
