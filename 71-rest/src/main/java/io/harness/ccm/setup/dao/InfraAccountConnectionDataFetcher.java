package io.harness.ccm.setup.dao;

import com.google.inject.Inject;

import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.ccm.setup.util.InfraSetUpUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.datafetcher.AccountThreadLocal;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class InfraAccountConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLInfraAccountConnectionData, QLInfraType> {
  @Inject private MainConfiguration mainConfiguration;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLInfraAccountConnectionData fetchConnection(QLInfraType parameters) {
    CESetUpConfig ceSetUpConfig = mainConfiguration.getCeSetUpConfig();

    String customerAccountId = getAccountId();
    if (parameters.getInfraType() == QLInfraTypesEnum.AWS) {
      String awsExternalId = InfraSetUpUtils.getAwsExternalId(ceSetUpConfig.getAwsAccountId(), customerAccountId);
      return QLInfraAccountConnectionData.builder()
          .externalId(awsExternalId)
          .harnessAccountId(ceSetUpConfig.getAwsAccountId())
          .cloudFormationTemplateLink(ceSetUpConfig.getCloudFormationTemplateLink())
          .build();
    }
    return QLInfraAccountConnectionData.builder().build();
  }

  public String getAccountId() {
    return AccountThreadLocal.get();
  }
}
