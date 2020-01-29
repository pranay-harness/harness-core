package software.wings.graphql.datafetcher.ssoProvider;

import lombok.experimental.UtilityClass;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOType;
import software.wings.graphql.schema.type.QLSSOProvider.QLSSOProviderBuilder;
import software.wings.graphql.schema.type.aggregation.ssoProvider.QLSSOType;

@UtilityClass
public class SSOProviderController {
  public QLSSOProviderBuilder populateSSOProvider(SSOSettings ssoProvider, QLSSOProviderBuilder builder) {
    QLSSOType ssoType = null;
    if (ssoProvider.getType() == SSOType.LDAP) {
      ssoType = QLSSOType.LDAP;
    }
    if (ssoProvider.getType() == SSOType.SAML) {
      ssoType = QLSSOType.SAML;
    }
    return builder.id(ssoProvider.getUuid()).name(ssoProvider.getDisplayName()).ssoType(ssoType);
  }
}
