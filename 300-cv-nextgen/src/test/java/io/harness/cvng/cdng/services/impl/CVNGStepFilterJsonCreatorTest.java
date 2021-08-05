package io.harness.cvng.cdng.services.impl;

import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.cdng.beans.TestVerificationJobSpec;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.contracts.plan.SetupMetadata;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext.FilterCreationContextBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class CVNGStepFilterJsonCreatorTest extends CvNextGenTestBase {
  @Inject private CVNGStepFilterJsonCreator cvngStepFilterJsonCreator;
  @Inject private MonitoredServiceService monitoredServiceService;
  @Inject private MetricPackService metricPackService;
  private BuilderFactory builderFactory;
  private String accountId;
  private String projectIdentifier;
  private String orgIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;
  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
    accountId = builderFactory.getContext().getAccountId();
    projectIdentifier = builderFactory.getContext().getProjectIdentifier();
    orgIdentifier = builderFactory.getContext().getOrgIdentifier();
    serviceIdentifier = builderFactory.getContext().getServiceIdentifier();
    envIdentifier = builderFactory.getContext().getEnvIdentifier();
    metricPackService.createDefaultMetricPackAndThresholds(accountId, orgIdentifier, projectIdentifier);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleNode_monitoredServiceDoesNotExist() {
    assertThatThrownBy(
        ()
            -> cvngStepFilterJsonCreator.handleNode(FilterCreationContext.builder()
                                                        .currentField(getYamlField())
                                                        .setupMetadata(SetupMetadata.newBuilder()
                                                                           .setAccountId(accountId)
                                                                           .setOrgId(orgIdentifier)
                                                                           .setProjectId(projectIdentifier)
                                                                           .build())
                                                        .build(),
                StepElementConfig.builder().stepSpecType(builderFactory.cvngStepInfoBuilder().build()).build()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("MonitoredService does not exist for service %s and env %s", serviceIdentifier, envIdentifier);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleNode_valid() throws IOException {
    MonitoredServiceDTO monitoredServiceDTO = builderFactory.monitoredServiceDTOBuilder().build();
    monitoredServiceService.create(accountId, monitoredServiceDTO);

    FilterCreationResponse filterCreationResponse =
        cvngStepFilterJsonCreator.handleNode(FilterCreationContext.builder()
                                                 .setupMetadata(SetupMetadata.newBuilder()
                                                                    .setAccountId(accountId)
                                                                    .setOrgId(orgIdentifier)
                                                                    .setProjectId(projectIdentifier)
                                                                    .build())
                                                 .currentField(getYamlField())
                                                 .build(),
            StepElementConfig.builder().stepSpecType(builderFactory.cvngStepInfoBuilder().build()).build());
    assertThat(filterCreationResponse.getReferredEntities()).hasSize(1);
    assertThat(filterCreationResponse.getReferredEntities().get(0).getIdentifierRef().getIdentifier().getValue())
        .isEqualTo(BuilderFactory.CONNECTOR_IDENTIFIER);
    assertThat(filterCreationResponse.getReferredEntities().get(0).getIdentifierRef().getProjectIdentifier().getValue())
        .isEqualTo(projectIdentifier);
    assertThat(filterCreationResponse.getReferredEntities().get(0).getIdentifierRef().getOrgIdentifier().getValue())
        .isEqualTo(orgIdentifier);
    assertThat(filterCreationResponse.getReferredEntities().get(0).getIdentifierRef().getAccountIdentifier().getValue())
        .isEqualTo(accountId);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleNode_whenServiceOrEnvIsRuntimeOrExpression() throws IOException {
    MonitoredServiceDTO monitoredServiceDTO = builderFactory.monitoredServiceDTOBuilder().build();
    monitoredServiceService.create(accountId, monitoredServiceDTO);
    YamlField yamlField = getYamlField("<+input>", "Prod");
    FilterCreationContextBuilder filterCreationContextBuilder =
        FilterCreationContext.builder().setupMetadata(SetupMetadata.newBuilder()
                                                          .setAccountId(accountId)
                                                          .setOrgId(orgIdentifier)
                                                          .setProjectId(projectIdentifier)
                                                          .build());
    FilterCreationResponse filterCreationResponse =
        cvngStepFilterJsonCreator.handleNode(filterCreationContextBuilder.currentField(yamlField).build(),
            StepElementConfig.builder().stepSpecType(builderFactory.cvngStepInfoBuilder().build()).build());

    assertThat(filterCreationResponse.getReferredEntities()).isEmpty();

    filterCreationResponse = cvngStepFilterJsonCreator.handleNode(
        filterCreationContextBuilder
            .currentField(getYamlField("verification", "<+serviceConfig.artifacts.primary.tag>"))
            .build(),
        StepElementConfig.builder().stepSpecType(builderFactory.cvngStepInfoBuilder().build()).build());
    assertThat(filterCreationResponse.getReferredEntities()).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleNode_invalidDurationField() {
    assertThatThrownBy(
        ()
            -> cvngStepFilterJsonCreator.handleNode(FilterCreationContext.builder()
                                                        .setupMetadata(SetupMetadata.newBuilder()
                                                                           .setAccountId(accountId)
                                                                           .setOrgId(orgIdentifier)
                                                                           .setProjectId(projectIdentifier)
                                                                           .build())
                                                        .currentField(getYamlField())
                                                        .build(),
                StepElementConfig.builder()
                    .stepSpecType(builderFactory.cvngStepInfoBuilder()
                                      .spec(TestVerificationJobSpec.builder()
                                                .duration(ParameterField.createValueField("5mm"))
                                                .deploymentTag(ParameterField.createValueField("build#1"))
                                                .sensitivity(ParameterField.createValueField("Low"))
                                                .build())
                                      .build())
                    .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("can not parse duration please check format for duration., ex: 5m, 10m etc.");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleNode_durationIsExpression() throws IOException {
    MonitoredServiceDTO monitoredServiceDTO = builderFactory.monitoredServiceDTOBuilder().build();
    monitoredServiceService.create(accountId, monitoredServiceDTO);
    FilterCreationResponse filterCreationResponse =
        cvngStepFilterJsonCreator.handleNode(FilterCreationContext.builder()
                                                 .setupMetadata(SetupMetadata.newBuilder()
                                                                    .setAccountId(accountId)
                                                                    .setOrgId(orgIdentifier)
                                                                    .setProjectId(projectIdentifier)
                                                                    .build())
                                                 .currentField(getYamlField())
                                                 .build(),
            StepElementConfig.builder()
                .stepSpecType(
                    builderFactory.cvngStepInfoBuilder()
                        .spec(TestVerificationJobSpec.builder()
                                  .duration(ParameterField.createExpressionField(true, "<+step.input>", null, true))
                                  .deploymentTag(ParameterField.createValueField("build#1"))
                                  .sensitivity(ParameterField.createValueField("Low"))
                                  .build())
                        .build())
                .build());
    assertThat(filterCreationResponse.getReferredEntities()).hasSize(1);
  }

  public YamlField getYamlField() throws IOException {
    return getYamlField(serviceIdentifier, envIdentifier);
  }

  public YamlField getYamlField(String serviceRef, String envRef) throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline-test.yml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    yamlContent = yamlContent.replace("$serviceRef", serviceRef);
    yamlContent = yamlContent.replace("$environmentRef", envRef);
    YamlField yamlField = YamlUtils.readTree(YamlUtils.injectUuid(yamlContent));
    YamlNode pipelineNode = yamlField.getNode().getField("pipeline").getNode();
    YamlField stagesNode = pipelineNode.getField("stages");
    YamlNode stage1Node = stagesNode.getNode().asArray().get(0).getField("stage").getNode();
    YamlNode stepsNode =
        stage1Node.getField("spec").getNode().getField("execution").getNode().getField("steps").getNode();
    YamlNode step1Node = stepsNode.asArray().get(0).getField("step").getNode();
    return new YamlField(step1Node);
  }
}