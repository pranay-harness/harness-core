package io.harness.cdng.infra.steps;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
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

    Infrastructure infrastructureSpec =
        K8SDirectInfrastructure.builder().connectorId(connector).namespace(namespace).build();

    InfraMapping expectedInfraMapping = K8sDirectInfraMapping.builder()
                                            .serviceIdentifier(serviceIdentifier)
                                            .k8sConnector(connector)
                                            .namespace(namespace)
                                            .build();

    InfraMapping infraMapping = infrastructureStep.createInfraMappingObject(serviceIdentifier, infrastructureSpec);
    assertThat(infraMapping).isEqualTo(expectedInfraMapping);
  }
}