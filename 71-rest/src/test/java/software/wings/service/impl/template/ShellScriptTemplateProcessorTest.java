package software.wings.service.impl.template;

import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.LINKED_TEMPLATE_UUIDS_KEY;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.TEXT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.LATEST_TAG;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.inject.Inject;

import com.mongodb.DBCursor;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.shell.ScriptType;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.template.TemplateService;

public class ShellScriptTemplateProcessorTest extends TemplateBaseTest {
  @Mock private WorkflowService workflowService;

  @Mock private MorphiaIterator<Workflow, Workflow> workflowIterator;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private Query<Workflow> query;
  @Mock private FieldEnd end;
  @Mock private DBCursor dbCursor;

  @Inject private ShellScriptTemplateProcessor shellScriptTemplateProcessor;
  @Inject private TemplateService templateService;

  @Test
  @Category(UnitTests.class)
  public void shouldLoadDefaultTemplates() {
    templateService.loadDefaultTemplates(TemplateType.SHELL_SCRIPT, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSaveShellScriptTemplate() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    ShellScriptTemplate shellScriptTemplate = ShellScriptTemplate.builder()
                                                  .scriptType(ScriptType.BASH.name())
                                                  .scriptString("echo ${var1}\n"
                                                      + "export A=\"aaa\"\n"
                                                      + "export B=\"bbb\"")
                                                  .outputVars("A,B")
                                                  .build();
    Template template = Template.builder()
                            .templateObject(shellScriptTemplate)
                            .folderId(parentFolder.getUuid())
                            .appId(GLOBAL_APP_ID)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .name("Sample Script")
                            .variables(asList(aVariable().withType(TEXT).withName("var1").withMandatory(true).build()))
                            .build();
    Template savedTemplate = templateService.save(template);
    assertSavedTemplate(template, savedTemplate);
    ShellScriptTemplate savedShellScriptTemplate = (ShellScriptTemplate) savedTemplate.getTemplateObject();
    assertThat(savedShellScriptTemplate).isNotNull();
    assertThat(savedShellScriptTemplate.getTimeoutMillis()).isEqualTo(600000);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateShellScriptTemplate() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    Template template = getTemplate(parentFolder);

    Template savedTemplate = templateService.save(template);

    assertSavedTemplate(template, savedTemplate);

    ShellScriptTemplate savedShellScriptTemplate = (ShellScriptTemplate) savedTemplate.getTemplateObject();
    assertThat(savedShellScriptTemplate).isNotNull();
    assertThat(savedShellScriptTemplate.getScriptString()).isNotEmpty();

    ShellScriptTemplate updatedShellScriptTemplate = ShellScriptTemplate.builder().timeoutMillis(300000).build();
    savedTemplate.setTemplateObject(updatedShellScriptTemplate);
    Template updatedTemplate = templateService.update(savedTemplate);

    updatedShellScriptTemplate = (ShellScriptTemplate) updatedTemplate.getTemplateObject();
    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(updatedTemplate.getVersion()).isEqualTo(2L);
    assertThat(updatedTemplate.getTemplateObject()).isNotNull();
    assertThat(((ShellScriptTemplate) updatedTemplate.getTemplateObject()).getTimeoutMillis()).isEqualTo(300000);
    assertThat(updatedTemplate.getVariables()).extracting("name").contains("var1");
    assertThat(updatedShellScriptTemplate).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotUpdateEntitiesIfNotLinked() {
    Template savedTemplate = createShellScriptTemplate();

    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withUuid(WORKFLOW_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .infraMappingId(INFRA_MAPPING_ID)
                                          .serviceId(SERVICE_ID)
                                          .deploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .withLinkedTemplateUuids(asList(savedTemplate.getUuid()))
            .build();

    on(shellScriptTemplateProcessor).set("wingsPersistence", wingsPersistence);
    on(shellScriptTemplateProcessor).set("workflowService", workflowService);

    when(wingsPersistence.createQuery(Workflow.class, excludeAuthority)).thenReturn(query);

    when(query.field(LINKED_TEMPLATE_UUIDS_KEY)).thenReturn(end);
    when(end.contains(savedTemplate.getUuid())).thenReturn(query);
    when(query.fetch()).thenReturn(workflowIterator);
    when(workflowIterator.getCursor()).thenReturn(dbCursor);
    when(workflowIterator.hasNext()).thenReturn(true).thenReturn(false);

    when(workflowIterator.next()).thenReturn(workflow);
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);

    templateService.updateLinkedEntities(savedTemplate);

    verify(workflowService).readWorkflow(APP_ID, WORKFLOW_ID);
    verify(workflowService, times(0)).updateWorkflow(workflow);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateEntitiesLinked() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    Template template = getTemplate(parentFolder);

    Template savedTemplate = templateService.save(template);

    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getVariables()).extracting("name").contains("var1");

    ShellScriptTemplate savedShellScriptTemplate = (ShellScriptTemplate) savedTemplate.getTemplateObject();
    assertThat(savedShellScriptTemplate).isNotNull();
    GraphNode step = shellScriptTemplateProcessor.constructEntityFromTemplate(savedTemplate);
    step.setTemplateVersion(LATEST_TAG);

    final Workflow workflow = generateWorkflow(savedTemplate, step);

    on(shellScriptTemplateProcessor).set("wingsPersistence", wingsPersistence);
    on(shellScriptTemplateProcessor).set("workflowService", workflowService);

    when(wingsPersistence.createQuery(Workflow.class, excludeAuthority)).thenReturn(query);

    when(query.field(LINKED_TEMPLATE_UUIDS_KEY)).thenReturn(end);
    when(end.contains(savedTemplate.getUuid())).thenReturn(query);
    when(query.fetch()).thenReturn(workflowIterator);
    when(workflowIterator.getCursor()).thenReturn(dbCursor);
    when(workflowIterator.hasNext()).thenReturn(true).thenReturn(false);
    when(workflowIterator.next()).thenReturn(workflow);

    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);

    templateService.updateLinkedEntities(savedTemplate);

    verify(workflowService).readWorkflow(APP_ID, WORKFLOW_ID);
    verify(workflowService).updateWorkflow(workflow);
  }

  private void assertSavedTemplate(Template template, Template savedTemplate) {
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getKeywords()).isNotEmpty();
    assertThat(savedTemplate.getKeywords()).contains(template.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    assertThat(savedTemplate.getVariables()).isNotEmpty();
    assertThat(savedTemplate.getVariables()).extracting("name").contains("var1");
  }

  private Template getTemplate(TemplateFolder parentFolder) {
    ShellScriptTemplate httpTemplate =
        ShellScriptTemplate.builder()
            .scriptType("BASH")
            .scriptString("echo \"Hello World ${var1}\"\n export A=\"aaa\"\n export B=\"bbb\"")
            .outputVars("A,B")
            .build();
    return Template.builder()
        .templateObject(httpTemplate)
        .folderId(parentFolder.getUuid())
        .appId(GLOBAL_APP_ID)
        .accountId(GLOBAL_ACCOUNT_ID)
        .name("Sample Script")
        .variables(asList(aVariable().withType(TEXT).withName("var1").withMandatory(true).build()))
        .build();
  }

  private Template createShellScriptTemplate() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    Template template = getTemplate(parentFolder);

    Template savedTemplate = templateService.save(template);

    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getVariables()).extracting("name").contains("var1");

    ShellScriptTemplate savedHttpTemplate = (ShellScriptTemplate) savedTemplate.getTemplateObject();
    assertThat(savedHttpTemplate).isNotNull();
    assertThat(savedHttpTemplate.getTimeoutMillis()).isEqualTo(600000);
    return savedTemplate;
  }
}
