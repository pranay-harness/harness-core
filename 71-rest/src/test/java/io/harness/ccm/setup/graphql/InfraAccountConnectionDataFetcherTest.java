package io.harness.ccm.setup.graphql;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.app.MainConfiguration;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;

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
