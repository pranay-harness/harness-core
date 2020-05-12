package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.sm.InfraMappingSummary.Builder.anInfraMappingSummary;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.sm.InfraDefinitionSummary;

import java.util.List;

public class InfraMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromElementExecutionSummary() {
    assertThat(InfraMetadata.fromElementExecutionSummary(null, true)).isNull();

    List<InfraMetadata> infraMetadataList =
        InfraMetadata.fromElementExecutionSummary(anElementExecutionSummary()
                                                      .withInfraMappingSummaries(asList(null,
                                                          anInfraMappingSummary()
                                                              .withDisplayName("n")
                                                              .withComputerProviderName("cpn")
                                                              .withComputerProviderType(CloudProviderType.AWS.name())
                                                              .withDeploymentType(DeploymentType.SSH.getDisplayName())
                                                              .build()))
                                                      .build(),
            false);
    assertThat(infraMetadataList).isNotNull();
    assertThat(infraMetadataList.size()).isEqualTo(1);
    validateInfraMetadata(infraMetadataList.get(0));

    infraMetadataList = InfraMetadata.fromElementExecutionSummary(anElementExecutionSummary()
                                                                      .withInfraDefinitionSummaries(asList(null,
                                                                          InfraDefinitionSummary.builder()
                                                                              .displayName("n")
                                                                              .cloudProviderName("cpn")
                                                                              .cloudProviderType(CloudProviderType.AWS)
                                                                              .deploymentType(DeploymentType.SSH)
                                                                              .build()))
                                                                      .build(),
        true);
    assertThat(infraMetadataList).isNotNull();
    assertThat(infraMetadataList.size()).isEqualTo(1);
    validateInfraMetadata(infraMetadataList.get(0));
  }

  private void validateInfraMetadata(InfraMetadata infraMetadata) {
    assertThat(infraMetadata).isNotNull();
    assertThat(infraMetadata.getName()).isEqualTo("n");
    assertThat(infraMetadata.getCloudProviderName()).isEqualTo("cpn");
    assertThat(infraMetadata.getCloudProviderType()).isEqualTo(CloudProviderType.AWS.name());
    assertThat(infraMetadata.getDeploymentType()).isEqualTo(DeploymentType.SSH.getDisplayName());
  }
}
