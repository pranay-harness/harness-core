package io.harness.event.grpc.auth;

import com.google.inject.Inject;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.AuthService;

/**
 * {@link ServerInterceptor} that validates the delegate token, and populates context with accountId before calling the
 * rpc implementation on server-side.
 */
@Slf4j
public class DelegateAuthServerInterceptor implements ServerInterceptor {
  private static final ServerCall.Listener NOOP_LISTENER = new ServerCall.Listener() {};
  private final AuthService authService;

  @Inject
  public DelegateAuthServerInterceptor(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> next) {
    String accountId = metadata.get(DelegateAuthCallCredentials.ACCOUNT_ID_METADATA_KEY);
    String token = metadata.get(DelegateAuthCallCredentials.TOKEN_METADATA_KEY);
    @SuppressWarnings("unchecked") Listener<ReqT> noopListener = NOOP_LISTENER;
    if (accountId == null) {
      logger.warn("No account id in metadata. Token verification failed");
      call.close(Status.UNAUTHENTICATED.withDescription("Account id missing"), metadata);
      return noopListener;
    }
    if (token == null) {
      logger.warn("No token in metadata. Token verification failed");
      call.close(Status.UNAUTHENTICATED.withDescription("Token missing"), metadata);
      return noopListener;
    }
    Context ctx;
    try {
      authService.validateDelegateToken(accountId, token);
      ctx = Context.current().withValue(DelegateAuthCallCredentials.ACCOUNT_ID_CTX_KEY, accountId);
    } catch (Exception e) {
      logger.warn("Token verification failed. Unauthenticated");
      call.close(Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e), metadata);
      return noopListener;
    }
    return Contexts.interceptCall(ctx, call, metadata, next);
  }
}
