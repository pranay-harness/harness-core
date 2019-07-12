package io.harness.event.grpc.auth;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static io.harness.network.Localhost.getLocalHostName;

import io.grpc.CallCredentials;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.SecurityLevel;
import io.grpc.Status;
import io.grpc.StatusException;
import io.harness.security.TokenGenerator;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executor;

/**
 * {@link CallCredentials} that adds delegate token to the request before calling the manager.
 */
@Slf4j
public class DelegateAuthCallCredentials extends CallCredentials {
  static final Metadata.Key<String> TOKEN_METADATA_KEY = Metadata.Key.of("token", ASCII_STRING_MARSHALLER);
  static final Metadata.Key<String> ACCOUNT_ID_METADATA_KEY = Metadata.Key.of("accountId", ASCII_STRING_MARSHALLER);
  public static final Context.Key<String> ACCOUNT_ID_CTX_KEY = Context.key("accountId");

  private final TokenGenerator tokenGenerator;
  private final String accountId;
  private final boolean requirePrivacy;

  public DelegateAuthCallCredentials(TokenGenerator tokenGenerator, String accountId, boolean requirePrivacy) {
    this.tokenGenerator = tokenGenerator;
    this.accountId = accountId;
    this.requirePrivacy = requirePrivacy;
  }

  @Override
  public void applyRequestMetadata(RequestInfo info, Executor appExecutor, MetadataApplier applier) {
    SecurityLevel security = info.getSecurityLevel();
    if (requirePrivacy && security != SecurityLevel.PRIVACY_AND_INTEGRITY) {
      logger.warn("Not adding token on insecure channel");
      applier.fail(Status.UNAUTHENTICATED.withDescription(
          "Including delegate credentials require channel with PRIVACY_AND_INTEGRITY security level. Observed security level: "
          + security));
    }
    String authority = checkNotNull(info.getAuthority(), "authority");
    final URI uri;
    try {
      uri = serviceUri(authority, info.getMethodDescriptor());
    } catch (StatusException e) {
      applier.fail(e.getStatus());
      return;
    }
    String token = tokenGenerator.getToken(uri.toString(), getLocalHostName());
    Metadata headers = new Metadata();
    headers.put(ACCOUNT_ID_METADATA_KEY, accountId);
    headers.put(TOKEN_METADATA_KEY, token);
    applier.apply(headers);
  }

  @Override
  public void thisUsesUnstableApi() {}

  private static URI serviceUri(String authority, MethodDescriptor<?, ?> method) throws StatusException {
    // Always use HTTPS, by definition.
    final String scheme = "https";
    String path = "/" + method.getServiceName();
    URI uri;
    try {
      uri = new URI(scheme, authority, path, null, null);
    } catch (URISyntaxException e) {
      logger.warn("Failed to construct service uri");
      throw Status.UNAUTHENTICATED.withDescription("Unable to construct service URI for auth")
          .withCause(e)
          .asException();
    }
    return uri;
  }
}
