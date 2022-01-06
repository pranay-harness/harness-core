/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ssoProvider;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOType;
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
