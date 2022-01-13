/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.rule.Owner;

import software.wings.app.MainConfiguration;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class InfraAccountConnectionDataFetcherTest extends AbstractDataFetcherTestBase {
  @InjectMocks InfraAccountConnectionDataFetcher infraAccountConnectionDataFetcher;

  @Mock private MainConfiguration mainConfiguration;

  private final String awsAccountId = "HARNESS_ACCOUNT_ID";
  private final String cloudFormationTemplateLink = "CLOUD_FORMATION_TEMPLATE_LINK";

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testFetchConnection() {
    when(mainConfiguration.getCeSetUpConfig())
        .thenReturn(CESetUpConfig.builder()
                        .awsAccountId(awsAccountId)
                        .masterAccountCloudFormationTemplateLink(cloudFormationTemplateLink)
                        .linkedAccountCloudFormationTemplateLink(cloudFormationTemplateLink)
                        .build());
    QLInfraType qlInfraType = QLInfraType.builder().infraType(QLInfraTypesEnum.AWS).build();
    QLInfraAccountConnectionData qlInfraAccountConnectionData =
        infraAccountConnectionDataFetcher.fetch(qlInfraType, ACCOUNT1_ID);
    assertThat(qlInfraAccountConnectionData.getMasterAccountCloudFormationTemplateLink())
        .isEqualTo(cloudFormationTemplateLink);
    assertThat(qlInfraAccountConnectionData.getLinkedAccountCloudFormationTemplateLink())
        .isEqualTo(cloudFormationTemplateLink);
    assertThat(qlInfraAccountConnectionData.getExternalId()).isEqualTo("harness:" + awsAccountId + ":" + ACCOUNT1_ID);
    assertThat(qlInfraAccountConnectionData.getHarnessAccountId()).isEqualTo(awsAccountId);
    assertThat(qlInfraAccountConnectionData.getLinkedAccountLaunchTemplateLink()).isNotNull();
    assertThat(qlInfraAccountConnectionData.getMasterAccountLaunchTemplateLink()).isNotNull();
  }
}
