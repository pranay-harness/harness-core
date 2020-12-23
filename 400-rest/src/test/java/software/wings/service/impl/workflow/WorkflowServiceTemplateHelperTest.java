package software.wings.service.impl.workflow;

import static io.harness.rule.OwnerRule.INDER;
import static io.harness.rule.OwnerRule.YOGESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStep.PhaseStepBuilder;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Variable;
import software.wings.beans.Variable.VariableBuilder;
import software.wings.beans.template.Template;
import software.wings.service.intfc.template.TemplateService;
import software.wings.sm.StepType;
import software.wings.sm.states.HelmDeployState.HelmDeployStateKeys;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class WorkflowServiceTemplateHelperTest extends WingsBaseTest {
  @Inject @InjectMocks private WorkflowServiceTemplateHelper workflowServiceTemplateHelper;
  @Mock private TemplateService templateService;

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testSaveHelmDeployState() {
    Map<String, Object> newProperties = new HashMap<>();
    newProperties.put(HelmDeployStateKeys.helmReleaseNamePrefix, "new-release-prod");
    PhaseStep newPhaseStep = PhaseStepBuilder.aPhaseStep(PhaseStepType.HELM_DEPLOY, "Helm Deploy")
                                 .addStep(GraphNode.builder()
                                              .id("id")
                                              .name("test-helm")
                                              .type(StepType.HELM_DEPLOY.toString())
                                              .properties(newProperties)
                                              .build())
                                 .addStep(GraphNode.builder()
                                              .id("id1")
                                              .name("test-helm-new")
                                              .type(StepType.HELM_DEPLOY.toString())
                                              .properties(Maps.newHashMap(newProperties))
                                              .build())
                                 .build();

    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(newPhaseStep, null, false);
    assertThat(newPhaseStep.getSteps().get(0).getProperties().get(HelmDeployStateKeys.helmReleaseNamePrefix))
        .isEqualTo("new-release-prod");
    assertThat(newPhaseStep.getSteps().get(1).getProperties().get(HelmDeployStateKeys.helmReleaseNamePrefix))
        .isEqualTo("new-release-prod");

    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(
        newPhaseStep, PhaseStepBuilder.aPhaseStep(PhaseStepType.HELM_DEPLOY).build(), false);
    assertThat(newPhaseStep.getSteps().get(0).getProperties().get(HelmDeployStateKeys.helmReleaseNamePrefix))
        .isEqualTo("new-release-prod");
    assertThat(newPhaseStep.getSteps().get(1).getProperties().get(HelmDeployStateKeys.helmReleaseNamePrefix))
        .isEqualTo("new-release-prod");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testSaveHelmDeployStateFromYaml() {
    Map<String, Object> newProperties = new HashMap<>();
    newProperties.put(HelmDeployStateKeys.helmReleaseNamePrefix, "new-release-prod");
    PhaseStep newPhaseStep = PhaseStepBuilder.aPhaseStep(PhaseStepType.HELM_DEPLOY, "Helm Deploy")
                                 .addStep(GraphNode.builder()
                                              .name("test-helm")
                                              .type(StepType.HELM_DEPLOY.toString())
                                              .properties(newProperties)
                                              .build())
                                 .addStep(GraphNode.builder()
                                              .name("test-helm-new")
                                              .type(StepType.HELM_DEPLOY.toString())
                                              .properties(Maps.newHashMap(newProperties))
                                              .build())
                                 .build();

    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(newPhaseStep, null, true);
    assertThat(newPhaseStep.getSteps().get(0).getProperties().get(HelmDeployStateKeys.helmReleaseNamePrefix))
        .isEqualTo("new-release-prod");
    assertThat(newPhaseStep.getSteps().get(1).getProperties().get(HelmDeployStateKeys.helmReleaseNamePrefix))
        .isEqualTo("new-release-prod");

    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(
        newPhaseStep, PhaseStepBuilder.aPhaseStep(PhaseStepType.HELM_DEPLOY).build(), true);
    assertThat(newPhaseStep.getSteps().get(0).getProperties().get(HelmDeployStateKeys.helmReleaseNamePrefix))
        .isEqualTo("new-release-prod");
    assertThat(newPhaseStep.getSteps().get(1).getProperties().get(HelmDeployStateKeys.helmReleaseNamePrefix))
        .isEqualTo("new-release-prod");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testUpdateHelmStateReleaseName() {
    Map<String, Object> oldProperties = new HashMap<>();
    oldProperties.put(HelmDeployStateKeys.helmReleaseNamePrefix, "release-prod");
    PhaseStep oldPhaseStep = PhaseStepBuilder.aPhaseStep(PhaseStepType.HELM_DEPLOY, "Helm Deploy")
                                 .addStep(GraphNode.builder()
                                              .id("id")
                                              .name("test-helm")
                                              .type(StepType.HELM_DEPLOY.toString())
                                              .properties(oldProperties)
                                              .build())
                                 .build();

    Map<String, Object> newProperties = new HashMap<>();
    newProperties.put(HelmDeployStateKeys.helmReleaseNamePrefix, "new-release-prod");
    PhaseStep newPhaseStep = PhaseStepBuilder.aPhaseStep(PhaseStepType.HELM_DEPLOY, "Helm Deploy")
                                 .addStep(GraphNode.builder()
                                              .id("id")
                                              .name("test-helm")
                                              .type(StepType.HELM_DEPLOY.toString())
                                              .properties(newProperties)
                                              .build())
                                 .addStep(GraphNode.builder()
                                              .id("id1")
                                              .name("test-helm-new")
                                              .type(StepType.HELM_DEPLOY.toString())
                                              .properties(Maps.newHashMap(newProperties))
                                              .build())
                                 .build();

    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(newPhaseStep, oldPhaseStep, false);
    assertThat(newPhaseStep.getSteps().get(0).getProperties().get(HelmDeployStateKeys.helmReleaseNamePrefix))
        .isEqualTo("release-prod");
    assertThat(newPhaseStep.getSteps().get(1).getProperties().get(HelmDeployStateKeys.helmReleaseNamePrefix))
        .isEqualTo("new-release-prod");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testUpdateHelmStateReleaseNameFromYaml() {
    Map<String, Object> oldProperties = new HashMap<>();
    oldProperties.put(HelmDeployStateKeys.helmReleaseNamePrefix, "release-prod");
    PhaseStep oldPhaseStep = PhaseStepBuilder.aPhaseStep(PhaseStepType.HELM_DEPLOY, "Helm Deploy")
                                 .addStep(GraphNode.builder()
                                              .id("id")
                                              .name("test-helm")
                                              .type(StepType.HELM_DEPLOY.toString())
                                              .properties(oldProperties)
                                              .build())
                                 .build();

    Map<String, Object> newProperties = new HashMap<>();
    newProperties.put(HelmDeployStateKeys.helmReleaseNamePrefix, "new-release-prod");
    PhaseStep newPhaseStep = PhaseStepBuilder.aPhaseStep(PhaseStepType.HELM_DEPLOY, "Helm Deploy")
                                 .addStep(GraphNode.builder()
                                              .name("test-helm")
                                              .type(StepType.HELM_DEPLOY.toString())
                                              .properties(newProperties)
                                              .build())
                                 .addStep(GraphNode.builder()
                                              .name("test-helm-new")
                                              .type(StepType.HELM_DEPLOY.toString())
                                              .properties(Maps.newHashMap(newProperties))
                                              .build())
                                 .build();

    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(newPhaseStep, oldPhaseStep, true);
    assertThat(newPhaseStep.getSteps().get(0).getProperties().get(HelmDeployStateKeys.helmReleaseNamePrefix))
        .isEqualTo("release-prod");
    assertThat(newPhaseStep.getSteps().get(1).getProperties().get(HelmDeployStateKeys.helmReleaseNamePrefix))
        .isEqualTo("new-release-prod");
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void shouldSetTemplateVariablesGivenByUser() {
    Variable var1 = VariableBuilder.aVariable().name("variable").value("from-yaml").build();
    Variable var2 = VariableBuilder.aVariable().name("variable").value("from-template").build();
    Variable httpVar1 = VariableBuilder.aVariable().name("http-variable").value("http-from-yaml").build();
    Variable httpVar2 = VariableBuilder.aVariable().name("http-variable").value("http-from-template").build();
    PhaseStep newPhaseStep = PhaseStepBuilder.aPhaseStep(PhaseStepType.PRE_DEPLOYMENT, "Pre deployment")
                                 .addStep(GraphNode.builder()
                                              .name("test")
                                              .type(StepType.SHELL_SCRIPT.toString())
                                              .templateUuid("uuid")
                                              .templateVersion("latest")
                                              .templateVariables(Collections.singletonList(var1))
                                              .build())
                                 .addStep(GraphNode.builder()
                                              .name("http-template-test")
                                              .type(StepType.HTTP.toString())
                                              .templateUuid("uuid-2")
                                              .templateVersion("latest")
                                              .templateVariables(Collections.singletonList(httpVar1))
                                              .build())
                                 .build();

    Template template = Template.builder().build();
    GraphNode graphNode = GraphNode.builder().templateVariables(Collections.singletonList(var2)).build();
    GraphNode httpGraphNode = GraphNode.builder().templateVariables(Collections.singletonList(httpVar2)).build();
    when(templateService.get(anyString(), anyString())).thenReturn(template);
    when(templateService.constructEntityFromTemplate(template, EntityType.WORKFLOW)).thenAnswer(new Answer() {
      private int count = 0;
      @Override
      public Object answer(InvocationOnMock invocationOnMock) {
        if (count == 0) {
          count++;
          return graphNode;
        }
        return httpGraphNode;
      }
    });

    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(newPhaseStep, null, true);
    assertThat(newPhaseStep.getSteps()).isNotNull();
    assertThat(newPhaseStep.getSteps()).hasSize(2);
    assertThat(newPhaseStep.getSteps().get(0).getTemplateVariables()).isNotNull();
    assertThat(newPhaseStep.getSteps().get(0).getTemplateVariables().get(0).getValue()).isEqualTo("from-yaml");
    assertThat(newPhaseStep.getSteps().get(1).getTemplateVariables()).isNotNull();
    assertThat(newPhaseStep.getSteps().get(1).getTemplateVariables().get(0).getValue()).isEqualTo("http-from-yaml");
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void ignoreExtraVariablesAddedFromTemplate() {
    Variable var1 = VariableBuilder.aVariable().name("variable").value("from-yaml").build();
    Variable var2 = VariableBuilder.aVariable().name("variable").value("from-template").build();
    Variable httpVar1 = VariableBuilder.aVariable().name("http-variable").value("http-from-yaml").build();
    Variable httpVar2 = VariableBuilder.aVariable().name("http-variable").value("http-from-template").build();
    PhaseStep newPhaseStep = PhaseStepBuilder.aPhaseStep(PhaseStepType.PRE_DEPLOYMENT, "Pre deployment")
                                 .addStep(GraphNode.builder()
                                              .name("test")
                                              .type(StepType.SHELL_SCRIPT.toString())
                                              .templateUuid("uuid")
                                              .templateVersion("latest")
                                              .templateVariables(Arrays.asList(var1, httpVar1))
                                              .build())
                                 .addStep(GraphNode.builder()
                                              .name("http-template-test")
                                              .type(StepType.HTTP.toString())
                                              .templateUuid("uuid-2")
                                              .templateVersion("latest")
                                              .templateVariables(Arrays.asList(httpVar1, var1))
                                              .build())
                                 .build();

    Template template = Template.builder().build();
    GraphNode graphNode = GraphNode.builder().templateVariables(Collections.singletonList(var2)).build();
    GraphNode httpGraphNode = GraphNode.builder().templateVariables(Collections.singletonList(httpVar2)).build();
    when(templateService.get(anyString(), anyString())).thenReturn(template);
    when(templateService.constructEntityFromTemplate(template, EntityType.WORKFLOW)).thenAnswer(new Answer() {
      private int count = 0;
      @Override
      public Object answer(InvocationOnMock invocationOnMock) {
        if (count == 0) {
          count++;
          return graphNode;
        }
        return httpGraphNode;
      }
    });

    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(newPhaseStep, null, true);
    assertThat(newPhaseStep.getSteps()).isNotNull();
    assertThat(newPhaseStep.getSteps()).hasSize(2);
    assertThat(newPhaseStep.getSteps().get(0).getTemplateVariables()).isNotNull().hasSize(1);
    assertThat(newPhaseStep.getSteps().get(0).getTemplateVariables().get(0).getValue()).isEqualTo("from-yaml");
    assertThat(newPhaseStep.getSteps().get(1).getTemplateVariables()).isNotNull().hasSize(1);
    assertThat(newPhaseStep.getSteps().get(1).getTemplateVariables().get(0).getValue()).isEqualTo("http-from-yaml");
  }
}
