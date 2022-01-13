/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.auth;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.grpc.InterceptorPriority;
import io.harness.grpc.utils.GrpcAuthUtils;
import io.harness.security.ServiceTokenAuthenticator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@InterceptorPriority(20)
public class ServiceAuthServerInterceptor implements ServerInterceptor {
  private static final ServerCall.Listener NOOP_LISTENER = new ServerCall.Listener() {};
  private Set<String> includedGrpcServices;
  private Map<String, ServiceTokenAuthenticator> serviceIdToAuthenticatorMap;

  @Inject
  public ServiceAuthServerInterceptor(Map<String, ServiceInfo> services) {
    includedGrpcServices = new HashSet();
    serviceIdToAuthenticatorMap = new HashMap<>();
    for (Map.Entry<String, ServiceInfo> entry : services.entrySet()) {
      includedGrpcServices.add(entry.getKey());
      serviceIdToAuthenticatorMap.put(entry.getValue().getId(),
          ServiceTokenAuthenticator.builder().secretKey(entry.getValue().getSecret()).build());
    }
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
    if (excluded(serverCall)) {
      return Contexts.interceptCall(Context.current(), serverCall, metadata, serverCallHandler);
    }

    // Urgent fix for DEL-1954. DelegateAuthServerInterceptor already authorized the call coming from delegate agent.
    if (GrpcAuthUtils.isAuthenticatedWithAccountId(metadata)) {
      return Contexts.interceptCall(Context.current(), serverCall, metadata, serverCallHandler);
    }

    final String token = GrpcAuthUtils.getTokenFromRequest(metadata).orElse(null);

    @SuppressWarnings("unchecked") ServerCall.Listener<ReqT> noopListener = NOOP_LISTENER;
    if (token == null || isBlank(token)) {
      log.warn("No token in metadata. Token verification failed");
      serverCall.close(Status.UNAUTHENTICATED.withDescription("Token missing"), metadata);
      return noopListener;
    }

    final Optional<String> serviceIdOpt = GrpcAuthUtils.getServiceIdFromRequest(metadata);
    if (!serviceIdOpt.isPresent() || isBlank(serviceIdOpt.get())) {
      log.warn("No serviceId in metadata. Token verification failed");
      serverCall.close(Status.UNAUTHENTICATED.withDescription("serviceId missing"), metadata);
      return noopListener;
    }

    Context ctx;
    try {
      final String serviceId = serviceIdOpt.get().trim();
      final ServiceTokenAuthenticator serviceTokenAuthenticator = serviceIdToAuthenticatorMap.get(serviceId);
      if (serviceTokenAuthenticator == null) {
        log.warn("unsupported serviceId [{}]", serviceId);
        serverCall.close(Status.UNAUTHENTICATED.withDescription("unsupported serviceId =" + serviceId), metadata);
        return noopListener;
      }
      serviceTokenAuthenticator.authenticate(token);
      ctx = GrpcAuthUtils.newAuthenticatedContext();
    } catch (Exception e) {
      log.warn("Token verification failed. Unauthenticated", e);
      serverCall.close(Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e), metadata);
      return noopListener;
    }
    return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
  }

  private <RespT, ReqT> boolean excluded(ServerCall<ReqT, RespT> call) {
    return !includedGrpcServices.contains(call.getMethodDescriptor().getServiceName());
  }
}
