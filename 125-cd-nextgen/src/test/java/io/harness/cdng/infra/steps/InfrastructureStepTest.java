package io.harness.cdng.infra.steps;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.SAHIL;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
import io.harness.cdng.infra.beans.K8sGcpInfraMapping;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure.K8SDirectInfrastructureBuilder;
import io.harness.cdng.infra.yaml.K8sGcpInfrastructure;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.yaml.ParameterField;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;

import com.google.inject.name.Named;
import java.util.Collections;
import java.util.HashMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(CDP)
public class InfrastructureStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock EnvironmentService environmentService;

  @InjectMocks private InfrastructureStep infrastructureStep;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testPrivelegaedAccessControlClient() throws NoSuchFieldException {
    assertThat(ReflectionUtils.getFieldByName(InfrastructureStep.class, "accessControlClient")
                   .getAnnotation(Named.class)
                   .value())
        .isEqualTo("PRIVILEGED");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testCreateInfraMappingObject() {
    String namespace = "namespace";
    String connector = "connector";

    Infrastructure infrastructureSpec = K8SDirectInfrastructure.builder()
                                            .connectorRef(ParameterField.createValueField(connector))
                                            .namespace(ParameterField.createValueField(namespace))
                                            .build();

    InfraMapping expectedInfraMapping =
        K8sDirectInfraMapping.builder().k8sConnector(connector).namespace(namespace).build();

    InfraMapping infraMapping = infrastructureStep.createInfraMappingObject(infrastructureSpec);
    assertThat(infraMapping).isEqualTo(expectedInfraMapping);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testCreateK8sGcpInfraMapping() {
    String namespace = "namespace";
    String connector = "connector";
    String cluster = "cluster";

    Infrastructure infrastructureSpec = K8sGcpInfrastructure.builder()
                                            .connectorRef(ParameterField.createValueField(connector))
                                            .namespace(ParameterField.createValueField(namespace))
                                            .cluster(ParameterField.createValueField(cluster))
                                            .build();

    InfraMapping expectedInfraMapping =
        K8sGcpInfraMapping.builder().gcpConnector(connector).namespace(namespace).cluster(cluster).build();

    InfraMapping infraMapping = infrastructureStep.createInfraMappingObject(infrastructureSpec);
    assertThat(infraMapping).isEqualTo(expectedInfraMapping);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testProcessEnvironment() {
    // TODO this test is not asserting anything.
    HashMap<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(SetupAbstractionKeys.accountId, "accountId");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "projectId");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "orgId");
    Ambiance ambiance = Ambiance.newBuilder().putAllSetupAbstractions(setupAbstractions).build();

    EnvironmentYaml environmentYaml = EnvironmentYaml.builder()
                                          .identifier("test-id")
                                          .name("test-id")
                                          .type(EnvironmentType.PreProduction)
                                          .tags(Collections.emptyMap())
                                          .build();

    PipelineInfrastructure pipelineInfrastructure =
        PipelineInfrastructure.builder().environment(environmentYaml).build();

    Environment expectedEnv = Environment.builder()
                                  .identifier("test-id")
                                  .type(EnvironmentType.PreProduction)
                                  .tags(Collections.emptyList())
                                  .build();

    doReturn(expectedEnv).when(environmentService).upsert(expectedEnv);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidateInfrastructure() {
    assertThatThrownBy(() -> infrastructureStep.validateInfrastructure(null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Infrastructure definition can't be null or empty");

    K8SDirectInfrastructureBuilder k8SDirectInfrastructureBuilder = K8SDirectInfrastructure.builder();
    infrastructureStep.validateInfrastructure(k8SDirectInfrastructureBuilder.build());

    k8SDirectInfrastructureBuilder.connectorRef(ParameterField.createValueField("connector"));
    infrastructureStep.validateInfrastructure(k8SDirectInfrastructureBuilder.build());

    k8SDirectInfrastructureBuilder.connectorRef(new ParameterField<>(null, true, "expression1", null, true));
    assertThatThrownBy(() -> infrastructureStep.validateInfrastructure(k8SDirectInfrastructureBuilder.build()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Unresolved Expression : [expression1]");

    k8SDirectInfrastructureBuilder.connectorRef(ParameterField.createValueField("connector"));
    k8SDirectInfrastructureBuilder.releaseName(new ParameterField<>(null, true, "expression2", null, true));
    assertThatThrownBy(() -> infrastructureStep.validateInfrastructure(k8SDirectInfrastructureBuilder.build()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Unresolved Expression : [expression2]");
  }
}
