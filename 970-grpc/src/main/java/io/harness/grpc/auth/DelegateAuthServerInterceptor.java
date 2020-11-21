package io.harness.grpc.auth;

import io.harness.grpc.InterceptorPriority;
import io.harness.grpc.utils.GrpcAuthUtils;
import io.harness.security.TokenAuthenticator;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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
 * {@link ServerInterceptor} that validates the delegate token, and populates context with accountId before calling the
 * rpc implementation on server-side.
 */
@Slf4j
@Singleton
@InterceptorPriority(10)
public class DelegateAuthServerInterceptor implements ServerInterceptor {
  public static final Context.Key<String> ACCOUNT_ID_CTX_KEY = Context.key("accountId");
  private static final ServerCall.Listener NOOP_LISTENER = new ServerCall.Listener() {};
  private static final Set<String> INCLUDED_SERVICES = ImmutableSet.of("io.harness.perpetualtask.PerpetualTaskService",
      "io.harness.event.PingPongService", "io.harness.event.EventPublisher");

  private final TokenAuthenticator tokenAuthenticator;

  @Inject
  public DelegateAuthServerInterceptor(TokenAuthenticator tokenAuthenticator) {
    this.tokenAuthenticator = tokenAuthenticator;
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> next) {
    if (excluded(call)) {
      return Contexts.interceptCall(Context.current(), call, metadata, next);
    }

    String accountId = metadata.get(DelegateAuthCallCredentials.ACCOUNT_ID_METADATA_KEY);
    String token = metadata.get(DelegateAuthCallCredentials.TOKEN_METADATA_KEY);
    @SuppressWarnings("unchecked") Listener<ReqT> noopListener = NOOP_LISTENER;
    if (accountId == null) {
      log.warn("No account id in metadata. Token verification failed");
      call.close(Status.UNAUTHENTICATED.withDescription("Account id missing"), metadata);
      return noopListener;
    }
    if (token == null) {
      log.warn("No token in metadata. Token verification failed");
      call.close(Status.UNAUTHENTICATED.withDescription("Token missing"), metadata);
      return noopListener;
    }
    Context ctx;
    try {
      tokenAuthenticator.validateToken(accountId, token);
      ctx = GrpcAuthUtils.newAuthenticatedContext().withValue(ACCOUNT_ID_CTX_KEY, accountId);
    } catch (Exception e) {
      log.warn("Token verification failed. Unauthenticated", e);
      call.close(Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e), metadata);
      return noopListener;
    }
    return Contexts.interceptCall(ctx, call, metadata, next);
  }

  private <RespT, ReqT> boolean excluded(ServerCall<ReqT, RespT> call) {
    return !INCLUDED_SERVICES.contains(call.getMethodDescriptor().getServiceName());
  }
}
