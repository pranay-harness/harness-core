package io.harness.grpc.auth;

import com.google.inject.Singleton;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.SecurityLevel;
import io.grpc.Status;
import io.harness.security.TokenGenerator;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;

/**
 * {@link CallCredentials} that adds delegate token to the request before calling the manager.
 */
@Slf4j
@Singleton
public class DelegateAuthCallCredentials extends CallCredentials {
  static final Metadata.Key<String> TOKEN_METADATA_KEY = Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER);
  static final Metadata.Key<String> ACCOUNT_ID_METADATA_KEY =
      Metadata.Key.of("accountId", Metadata.ASCII_STRING_MARSHALLER);

  private final TokenGenerator tokenGenerator;
  private final String accountId;

  // Check if channel is encrypted before adding the token
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
      log.warn("Not adding token on insecure channel");
      applier.fail(Status.UNAUTHENTICATED.withDescription(
          "Including delegate credentials require channel with PRIVACY_AND_INTEGRITY security level. Observed security level: "
          + security));
    } else {
      String token = tokenGenerator.getToken("grpc-server", "grpc-client");
      Metadata headers = new Metadata();
      headers.put(ACCOUNT_ID_METADATA_KEY, accountId);
      headers.put(TOKEN_METADATA_KEY, token);
      applier.apply(headers);
    }
  }

  @Override
  public void thisUsesUnstableApi() {}
}
