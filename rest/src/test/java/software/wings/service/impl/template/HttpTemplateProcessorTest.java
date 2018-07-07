package software.wings.service.impl.template;

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
import static software.wings.beans.PhaseStepType.VERIFY_SERVICE;
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
import org.junit.Test;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.template.TemplateService;

import java.util.Arrays;

public class HttpTemplateProcessorTest extends TemplateBaseTest {
  @Mock private WorkflowService workflowService;

  @Mock private MorphiaIterator<Workflow, Workflow> workflowIterator;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private Query<Workflow> query;
  @Mock private FieldEnd end;
  @Mock private DBCursor dbCursor;

  @Inject private HttpTemplateProcessor httpTemplateProcessor;
  @Inject private TemplateService templateService;

  @Test
  public void shouldLoadDefaultTemplates() {
    templateService.loadDefaultTemplates(TemplateType.HTTP, GLOBAL_ACCOUNT_ID);
  }

  @Test
  public void shouldSaveHttpTemplate() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    HttpTemplate httpTemplate =
        HttpTemplate.builder().url("{Url}").method("GET").header("Authorization:${Header}").assertion("200 ok").build();
    Template template =
        Template.builder()
            .templateObject(httpTemplate)
            .folderId(parentFolder.getUuid())
            .appId(GLOBAL_APP_ID)
            .accountId(GLOBAL_ACCOUNT_ID)
            .name("Enable Instance")
            .variables(Arrays.asList(aVariable().withType(TEXT).withName("Url").withMandatory(true).build(),
                aVariable().withType(TEXT).withName("Header").withMandatory(true).build()))
            .build();
    Template savedTemplate = templateService.save(template);
    assertSavedTemplate(template, savedTemplate);
    HttpTemplate savedHttpTemplate = (HttpTemplate) savedTemplate.getTemplateObject();
    assertThat(savedHttpTemplate).isNotNull();
    assertThat(savedHttpTemplate.getAssertion()).isNotEmpty();
  }

  @Test
  public void shouldUpdateHttpTemplate() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    Template template = getTemplate(parentFolder);

    Template savedTemplate = templateService.save(template);

    assertSavedTemplate(template, savedTemplate);

    HttpTemplate savedHttpTemplate = (HttpTemplate) savedTemplate.getTemplateObject();
    assertThat(savedHttpTemplate).isNotNull();
    assertThat(savedHttpTemplate.getAssertion()).isNotEmpty();

    HttpTemplate updatedHttpTemplate = HttpTemplate.builder()
                                           .assertion(savedHttpTemplate.getAssertion())
                                           .url("http://${workflow.variables.F5_URL}/mgmt/tm/${foo}/members")
                                           .method(savedHttpTemplate.getMethod())
                                           .build();
    savedTemplate.setTemplateObject(updatedHttpTemplate);
    Template updatedTemplate = templateService.update(savedTemplate);

    updatedHttpTemplate = (HttpTemplate) updatedTemplate.getTemplateObject();
    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(updatedTemplate.getVersion()).isEqualTo(2L);
    assertThat(updatedTemplate.getTemplateObject()).isNotNull();
    assertThat(updatedTemplate.getVariables()).extracting("name").contains("Url", "Header");
    assertThat(updatedHttpTemplate).isNotNull();
  }

  private void assertSavedTemplate(Template template, Template savedTemplate) {
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getKeywords()).isNotEmpty();
    assertThat(savedTemplate.getKeywords()).contains(template.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    assertThat(savedTemplate.getVariables()).isNotEmpty();
    assertThat(savedTemplate.getVariables()).extracting("name").contains("Url", "Header");
  }

  @Test
  public void shouldNotUpdateEntitiesIfNotLinked() {
    Template savedTemplate = createHttpTemplate();

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
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .withLinkedTemplateUuids(Arrays.asList(savedTemplate.getUuid()))
            .build();

    on(httpTemplateProcessor).set("wingsPersistence", wingsPersistence);
    on(httpTemplateProcessor).set("workflowService", workflowService);

    when(wingsPersistence.createQuery(Workflow.class)).thenReturn(query);

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

  private Template createHttpTemplate() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    Template template = getTemplate(parentFolder);

    Template savedTemplate = templateService.save(template);

    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getVariables()).extracting("name").contains("Url", "Header");

    HttpTemplate savedHttpTemplate = (HttpTemplate) savedTemplate.getTemplateObject();
    assertThat(savedHttpTemplate).isNotNull();
    assertThat(savedHttpTemplate.getAssertion()).isNotEmpty();
    return savedTemplate;
  }

  private Template getTemplate(TemplateFolder parentFolder) {
    HttpTemplate httpTemplate = HttpTemplate.builder()
                                    .url("http://$workflow.variables.F5_URL}/mgmt/tm/${foo}/members")
                                    .method("GET")
                                    .body("{ \"kind\": ${LB_TYPE}}")
                                    .header("Authorization:${workflow.variables.F5_AUTH_BASE64}")
                                    .assertion("200 ok")
                                    .build();
    return Template.builder()
        .templateObject(httpTemplate)
        .folderId(parentFolder.getUuid())
        .appId(GLOBAL_APP_ID)
        .accountId(GLOBAL_ACCOUNT_ID)
        .name("Enable Instance")
        .variables(Arrays.asList(aVariable().withType(TEXT).withName("Url").withMandatory(true).build(),
            aVariable().withType(TEXT).withName("Header").withMandatory(true).build()))
        .build();
  }

  @Test
  public void shouldUpdateEntitiesLinked() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    Template template = getTemplate(parentFolder);

    Template savedTemplate = templateService.save(template);

    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getVariables()).extracting("name").contains("Url", "Header");

    HttpTemplate savedHttpTemplate = (HttpTemplate) savedTemplate.getTemplateObject();
    assertThat(savedHttpTemplate).isNotNull();
    assertThat(savedHttpTemplate.getAssertion()).isNotEmpty();
    GraphNode step = httpTemplateProcessor.constructEntityFromTemplate(savedTemplate);
    step.setTemplateVersion(LATEST_TAG);

    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withUuid(WORKFLOW_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).addStep(step).build())
                    .addWorkflowPhase(
                        aWorkflowPhase()
                            .withInfraMappingId(INFRA_MAPPING_ID)
                            .withServiceId(SERVICE_ID)
                            .withDeploymentType(SSH)
                            .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE).addStep(step).build())
                            .build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).addStep(step).build())
                    .build())
            .withLinkedTemplateUuids(Arrays.asList(savedTemplate.getUuid()))
            .build();

    on(httpTemplateProcessor).set("wingsPersistence", wingsPersistence);
    on(httpTemplateProcessor).set("workflowService", workflowService);

    when(wingsPersistence.createQuery(Workflow.class)).thenReturn(query);

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
}