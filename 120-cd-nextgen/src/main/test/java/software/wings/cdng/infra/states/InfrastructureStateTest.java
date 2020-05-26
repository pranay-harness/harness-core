package software.wings.cdng.infra.states;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.cdng.infra.beans.InfraDefinition;
import software.wings.cdng.infra.beans.InfraMapping;
import software.wings.cdng.infra.beans.K8sDirectInfraDefinition;
import software.wings.cdng.infra.beans.K8sDirectInfraMapping;

public class InfrastructureStateTest {
  private InfrastructureState infrastructureState = new InfrastructureState();

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testCreateInfraMappingObject() {
    String accountId = "accountId";
    String namespace = "namespace";
    String connector = "connector";
    String service = "service";

    InfraDefinition infraDefinition =
        K8sDirectInfraDefinition.builder()
            .accountId(accountId)
            .spec(K8sDirectInfraDefinition.Spec.builder().k8sConnector(connector).namespace(namespace).build())
            .build();

    InfraMapping expectedInfraMapping = K8sDirectInfraMapping.builder()
                                            .serviceName(service)
                                            .k8sConnector(connector)
                                            .namespace(namespace)
                                            .accountId(accountId)
                                            .build();

    InfraMapping infraMapping = infrastructureState.createInfraMappingObject(service, infraDefinition);
    assertThat(infraMapping).isEqualTo(expectedInfraMapping);
  }
}