package io.harness.grpc.utils;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.Context;
import io.grpc.Metadata;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GrpcAuthUtils {
  @VisibleForTesting
  public final Context.Key<Boolean> IS_AUTHENTICATED_CONTEXT_KEY =
      Context.keyWithDefault("isAuthenticated", Boolean.FALSE);
  private final io.grpc.Metadata.Key<String> SERVICE_ID_METADATA_KEY =
      Metadata.Key.of("serviceId", Metadata.ASCII_STRING_MARSHALLER);
  private static final Metadata.Key<String> TOKEN_METADATA_KEY =
      Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER);

  public boolean isAuthenticated() {
    return IS_AUTHENTICATED_CONTEXT_KEY.get();
  }

  public Context newAuthenticatedContext() {
    return Context.current().withValue(IS_AUTHENTICATED_CONTEXT_KEY, Boolean.TRUE);
  }

  public void setServiceAuthDetailsInRequest(@NotNull String serviceId, String token, Metadata metadata) {
    setServiceIdInRequest(serviceId, metadata);
    setTokenInRequest(token, metadata);
  }

  private void setServiceIdInRequest(@NotNull String serviceId, Metadata metadata) {
    metadata.put(SERVICE_ID_METADATA_KEY, serviceId);
  }

  private void setTokenInRequest(@NotNull String token, Metadata metadata) {
    metadata.put(TOKEN_METADATA_KEY, token);
  }

  public Optional<String> getServiceIdFromRequest(Metadata metadata) {
    return Optional.ofNullable(metadata.get(SERVICE_ID_METADATA_KEY));
  }

  public Optional<String> getTokenFromRequest(Metadata metadata) {
    return Optional.ofNullable(metadata.get(TOKEN_METADATA_KEY));
  }
}
