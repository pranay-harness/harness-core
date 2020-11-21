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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

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
    PhaseStep newPhaseStep = PhaseStepBuilder.aPhaseStep(PhaseStepType.PRE_DEPLOYMENT, "Pre deployment")
                                 .addStep(GraphNode.builder()
                                              .name("test")
                                              .type(StepType.SHELL_SCRIPT.toString())
                                              .templateUuid("uuid")
                                              .templateVersion("latest")
                                              .templateVariables(Collections.singletonList(var1))
                                              .build())
                                 .build();

    Template template = Template.builder().build();
    GraphNode graphNode = GraphNode.builder().templateVariables(Collections.singletonList(var2)).build();
    when(templateService.get(anyString(), anyString())).thenReturn(template);
    when(templateService.constructEntityFromTemplate(template, EntityType.WORKFLOW)).thenReturn(graphNode);

    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(newPhaseStep, null, true);
    assertThat(newPhaseStep.getSteps()).isNotNull();
    assertThat(newPhaseStep.getSteps().get(0).getTemplateVariables()).isNotNull();
    assertThat(newPhaseStep.getSteps().get(0).getTemplateVariables().get(0).getValue()).isEqualTo("from-yaml");
  }
}
