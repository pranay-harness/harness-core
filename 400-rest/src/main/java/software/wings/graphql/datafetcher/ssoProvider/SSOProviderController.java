package software.wings.graphql.datafetcher.ssoProvider;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.sso.SSOSettings;
import io.harness.beans.sso.SSOType;

import software.wings.graphql.schema.type.QLSSOProvider.QLSSOProviderBuilder;
import software.wings.graphql.schema.type.aggregation.ssoProvider.QLSSOType;

import lombok.experimental.UtilityClass;

@UtilityClass
@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
