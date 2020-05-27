package io.harness.cdng.infra.steps;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.InfraDefinition;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.K8sDirectInfraDefinition;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InfrastructureStepTest {
  private InfrastructureStep infrastructureStep = new InfrastructureStep();

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testCreateInfraMappingObject() {
    String accountId = "accountId";
    String namespace = "namespace";
    String connector = "connector";
    String serviceIdentifier = "serviceIdentifier";

    InfraDefinition infraDefinition =
        K8sDirectInfraDefinition.builder()
            .accountId(accountId)
            .spec(K8sDirectInfraDefinition.Spec.builder().k8sConnector(connector).namespace(namespace).build())
            .build();

    InfraMapping expectedInfraMapping = K8sDirectInfraMapping.builder()
                                            .serviceIdentifier(serviceIdentifier)
                                            .k8sConnector(connector)
                                            .namespace(namespace)
                                            .accountId(accountId)
                                            .build();

    InfraMapping infraMapping = infrastructureStep.createInfraMappingObject(serviceIdentifier, infraDefinition);
    assertThat(infraMapping).isEqualTo(expectedInfraMapping);
  }
}