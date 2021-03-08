package io.harness.cdng.infra.steps;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.HashMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class InfrastructureStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock EnvironmentService environmentService;

  @InjectMocks private InfrastructureStep infrastructureStep;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testCreateInfraMappingObject() {
    String namespace = "namespace";
    String connector = "connector";
    String serviceIdentifier = "serviceIdentifier";

    Infrastructure infrastructureSpec = K8SDirectInfrastructure.builder()
                                            .connectorRef(ParameterField.createValueField(connector))
                                            .namespace(ParameterField.createValueField(namespace))
                                            .build();

    InfraMapping expectedInfraMapping = K8sDirectInfraMapping.builder()
                                            .serviceIdentifier(serviceIdentifier)
                                            .k8sConnector(connector)
                                            .namespace(namespace)
                                            .build();

    InfraMapping infraMapping = infrastructureStep.createInfraMappingObject(serviceIdentifier, infrastructureSpec);
    assertThat(infraMapping).isEqualTo(expectedInfraMapping);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testProcessEnvironment() {
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

    EnvironmentOutcome environmentOutcome = EnvironmentOutcome.builder()
                                                .identifier("test-id")
                                                .name("test-id")
                                                .description("")
                                                .environmentType(EnvironmentType.PreProduction)
                                                .tags(Collections.emptyList())
                                                .build();
    doReturn(expectedEnv).when(environmentService).upsert(expectedEnv);

    EnvironmentOutcome actualEnvironmentOutcome =
        infrastructureStep.processEnvironment(ambiance, pipelineInfrastructure);

    assertThat(actualEnvironmentOutcome).isEqualTo(environmentOutcome);
  }
}
