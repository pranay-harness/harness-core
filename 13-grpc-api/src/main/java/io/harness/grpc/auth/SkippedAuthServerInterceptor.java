package io.harness.grpc.auth;

import com.google.inject.Singleton;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.harness.grpc.InterceptorPriority;
import io.harness.grpc.utils.GrpcAuthUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * ensures that the request is validated by some auth interceptors
 */
@Slf4j
@Singleton
@InterceptorPriority(Integer.MAX_VALUE)
public class SkippedAuthServerInterceptor implements ServerInterceptor {
  private static final Listener NOOP_LISTENER = new Listener() {};
  private final Set<String> excludedGrpcServices;

  public SkippedAuthServerInterceptor(Set<String> excludedGrpcServices) {
    this.excludedGrpcServices = excludedGrpcServices;
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> next) {
    if (GrpcAuthUtils.isAuthenticated() || excluded(call)) {
      return Contexts.interceptCall(Context.current(), call, metadata, next);
    }
    logger.warn("No Auth interceptor could validate this request. Please add one for this service");
    call.close(Status.UNAUTHENTICATED.withDescription("Unable to authenticate request"), metadata);
    return NOOP_LISTENER;
  }

  private <RespT, ReqT> boolean excluded(ServerCall<ReqT, RespT> call) {
    return excludedGrpcServices.contains(call.getMethodDescriptor().getServiceName());
  }
}
