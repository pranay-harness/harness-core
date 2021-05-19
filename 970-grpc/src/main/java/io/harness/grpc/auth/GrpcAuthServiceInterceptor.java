package io.harness.grpc.auth;

import io.harness.grpc.InterceptorPriority;
import io.harness.grpc.utils.GrpcAuthUtils;
import io.harness.security.JWTTokenServiceUtils;

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
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ServerInterceptor} that validates the delegate token, and populates context with accountId before calling the
 * rpc implementation on server-side.
 */
@Slf4j
@Singleton
@InterceptorPriority(10)
public class GrpcAuthServiceInterceptor implements ServerInterceptor {
  public static final Context.Key<String> ACCOUNT_ID_CTX_KEY = Context.key("accountId");
  private static final Listener NOOP_LISTENER = new Listener() {};
  private final Set<String> includedServices;
  private final Map<String, String> serviceIdToAuthToken;

  @Inject
  public GrpcAuthServiceInterceptor(@Named("grpc-services-with-auth") Set<String> includedServices,
      @Named("serviceAuthTokens") Map<String, String> serviceIdToAuthToken) {
    this.includedServices = includedServices;
    this.serviceIdToAuthToken = serviceIdToAuthToken;
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> next) {
    if (excluded(call)) {
      return Contexts.interceptCall(Context.current(), call, metadata, next);
    }

    String token = metadata.get(DelegateAuthCallCredentials.TOKEN_METADATA_KEY);
    @SuppressWarnings("unchecked") Listener<ReqT> noopListener = NOOP_LISTENER;
    if (token == null) {
      log.warn("No token in metadata. Token verification failed");
      call.close(Status.UNAUTHENTICATED.withDescription("Token missing"), metadata);
      return noopListener;
    }
    Context ctx;
    try {
      JWTTokenServiceUtils.isServiceAuthorizationValid(
          token, "IC04LYMBf1lDP5oeY4hupxd4HJhLmN6azUku3xEbeE3SUx5G3ZYzhbiwVtK4i7AmqyU9OZkwB4v8E9qM");
      ctx = GrpcAuthUtils.newAuthenticatedContext();
    } catch (Exception e) {
      log.warn("Token verification failed. Unauthenticated", e);
      call.close(Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e), metadata);
      return noopListener;
    }
    return Contexts.interceptCall(ctx, call, metadata, next);
  }

  private <RespT, ReqT> boolean excluded(ServerCall<ReqT, RespT> call) {
    return !includedServices.contains(call.getMethodDescriptor().getServiceName());
  }
}
