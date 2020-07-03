package io.harness.grpc.auth;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.harness.grpc.InterceptorPriority;
import io.harness.grpc.utils.GrpcAuthUtils;
import io.harness.security.ServiceTokenAuthenticator;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
@InterceptorPriority(20)
public class ServiceAuthServerInterceptor implements ServerInterceptor {
  private static final ServerCall.Listener NOOP_LISTENER = new ServerCall.Listener() {};
  private final Set<String> includedGrpcServices;
  private final Map<String, ServiceTokenAuthenticator> serviceIdToAuthenticatorMap;

  public ServiceAuthServerInterceptor(
      @NotNull Map<String, String> serviceIdToSecretKeyMap, Set<String> includedGrpcServices) {
    serviceIdToAuthenticatorMap = serviceIdToSecretKeyMap.entrySet().stream().collect(Collectors.toMap(
        Map.Entry::getKey, entry -> ServiceTokenAuthenticator.builder().secretKey(entry.getValue()).build()));
    this.includedGrpcServices = includedGrpcServices;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
    if (excluded(serverCall)) {
      return Contexts.interceptCall(Context.current(), serverCall, metadata, serverCallHandler);
    }

    final String token = GrpcAuthUtils.getTokenFromRequest(metadata).orElse(null);

    @SuppressWarnings("unchecked") ServerCall.Listener<ReqT> noopListener = NOOP_LISTENER;
    if (token == null || isBlank(token)) {
      logger.warn("No token in metadata. Token verification failed");
      serverCall.close(Status.UNAUTHENTICATED.withDescription("Token missing"), metadata);
      return noopListener;
    }

    final Optional<String> serviceIdOpt = GrpcAuthUtils.getServiceIdFromRequest(metadata);
    if (!serviceIdOpt.isPresent() || isBlank(serviceIdOpt.get())) {
      logger.warn("No serviceId in metadata. Token verification failed");
      serverCall.close(Status.UNAUTHENTICATED.withDescription("serviceId missing"), metadata);
      return noopListener;
    }

    Context ctx;
    try {
      final String serviceId = serviceIdOpt.get().trim();
      final ServiceTokenAuthenticator serviceTokenAuthenticator = serviceIdToAuthenticatorMap.get(serviceId);
      if (serviceTokenAuthenticator == null) {
        logger.warn("unsupported serviceId [{}]", serviceId);
        serverCall.close(Status.UNAUTHENTICATED.withDescription("unsupported serviceId =" + serviceId), metadata);
        return noopListener;
      }
      serviceTokenAuthenticator.authenticate(token);
      ctx = GrpcAuthUtils.newAuthenticatedContext();
    } catch (Exception e) {
      logger.warn("Token verification failed. Unauthenticated", e);
      serverCall.close(Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e), metadata);
      return noopListener;
    }
    return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
  }

  private <RespT, ReqT> boolean excluded(ServerCall<ReqT, RespT> call) {
    return !includedGrpcServices.contains(call.getMethodDescriptor().getServiceName());
  }
}
