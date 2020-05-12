package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.api.ServiceElement;
import software.wings.sm.InfraDefinitionSummary;

public class ServiceInfraSummaryMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromElementExecutionSummary() {
    assertThat(ServiceInfraSummaryMetadata.fromElementExecutionSummary(null, true)).isNull();
    assertThat(ServiceInfraSummaryMetadata.fromElementExecutionSummary(anElementExecutionSummary().build(), true))
        .isNull();

    ServiceInfraSummaryMetadata serviceInfraSummaryMetadata = ServiceInfraSummaryMetadata.fromElementExecutionSummary(
        anElementExecutionSummary()
            .withContextElement(ServiceElement.builder().name("n").build())
            .withInfraDefinitionSummaries(asList(null,
                InfraDefinitionSummary.builder()
                    .displayName("n")
                    .cloudProviderName("cpn")
                    .cloudProviderType(CloudProviderType.AWS)
                    .deploymentType(DeploymentType.SSH)
                    .build()))
            .withInstanceStatusSummaries(
                asList(null, anInstanceStatusSummary().withInstanceElement(anInstanceElement().build()).build(),
                    anInstanceStatusSummary().build()))
            .build(),
        true);
    assertThat(serviceInfraSummaryMetadata).isNotNull();
    assertThat(serviceInfraSummaryMetadata.getService()).isEqualTo("n");
    assertThat(serviceInfraSummaryMetadata.getInfrastructure()).isNotNull();
    assertThat(serviceInfraSummaryMetadata.getInfrastructure().size()).isEqualTo(1);

    InfraMetadata infraMetadata = serviceInfraSummaryMetadata.getInfrastructure().get(0);
    assertThat(infraMetadata).isNotNull();
    assertThat(infraMetadata.getName()).isEqualTo("n");
    assertThat(infraMetadata.getCloudProviderName()).isEqualTo("cpn");
    assertThat(infraMetadata.getCloudProviderType()).isEqualTo(CloudProviderType.AWS.name());
    assertThat(infraMetadata.getDeploymentType()).isEqualTo(DeploymentType.SSH.getDisplayName());

    assertThat(serviceInfraSummaryMetadata.getInstancesDeployed()).isEqualTo(1);
  }
}
