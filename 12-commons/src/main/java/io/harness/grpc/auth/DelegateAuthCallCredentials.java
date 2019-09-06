package io.harness.grpc.auth;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.SecurityLevel;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;

/**
 * {@link CallCredentials} that adds delegate token to the request before calling the manager.
 */
@Slf4j
public class DelegateAuthCallCredentials extends CallCredentials {
  static final Metadata.Key<String> TOKEN_METADATA_KEY = Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER);
  static final Metadata.Key<String> ACCOUNT_ID_METADATA_KEY =
      Metadata.Key.of("accountId", Metadata.ASCII_STRING_MARSHALLER);

  private final EventServiceTokenGenerator eventServiceTokenGenerator;
  private final String accountId;
  private final boolean requirePrivacy;

  public DelegateAuthCallCredentials(
      EventServiceTokenGenerator eventServiceTokenGenerator, String accountId, boolean requirePrivacy) {
    this.eventServiceTokenGenerator = eventServiceTokenGenerator;
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
    } else {
      String token = eventServiceTokenGenerator.getEventServiceToken();
      Metadata headers = new Metadata();
      headers.put(ACCOUNT_ID_METADATA_KEY, accountId);
      headers.put(TOKEN_METADATA_KEY, token);
      applier.apply(headers);
    }
  }

  @Override
  public void thisUsesUnstableApi() {}
}
