package software.wings.graphql.datafetcher.ssoProvider;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.sso.SSOSettings;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLSSOProviderQueryParameters;
import software.wings.graphql.schema.type.QLSSOProvider;
import software.wings.graphql.schema.type.QLSSOProvider.QLSSOProviderBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class SsoProviderDataFetcher extends AbstractObjectDataFetcher<QLSSOProvider, QLSSOProviderQueryParameters> {
  public static final String SSO_PROVIDER_DOES_NOT_EXIST_MSG = "SSO Provider does not exist";
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public QLSSOProvider fetch(QLSSOProviderQueryParameters qlQuery, String accountId) {
    SSOSettings ssoProvider = null;
    if (qlQuery.getSsoProviderId() != null) {
      ssoProvider = persistence.get(SSOSettings.class, qlQuery.getSsoProviderId());
    }
    if (ssoProvider == null) {
      throw new InvalidRequestException(SSO_PROVIDER_DOES_NOT_EXIST_MSG, WingsException.USER);
    }

    final QLSSOProviderBuilder builder = QLSSOProvider.builder();
    SSOProviderController.populateSSOProvider(ssoProvider, builder);
    return builder.build();
  }
}
