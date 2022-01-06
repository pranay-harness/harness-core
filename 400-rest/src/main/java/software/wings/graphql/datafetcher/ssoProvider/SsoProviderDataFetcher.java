/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ssoProvider;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_AUTHENTICATION_SETTINGS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;

import software.wings.beans.sso.SSOSettings;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLSSOProviderQueryParameters;
import software.wings.graphql.schema.type.QLSSOProvider;
import software.wings.graphql.schema.type.QLSSOProvider.QLSSOProviderBuilder;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class SsoProviderDataFetcher extends AbstractObjectDataFetcher<QLSSOProvider, QLSSOProviderQueryParameters> {
  private static final String SSO_PROVIDER_DOES_NOT_EXIST_MSG = "SSO Provider does not exist";
  @Inject private HPersistence persistence;

  @Override
  @AuthRule(permissionType = MANAGE_AUTHENTICATION_SETTINGS)
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
