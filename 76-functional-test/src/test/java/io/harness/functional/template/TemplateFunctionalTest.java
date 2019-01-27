package io.harness.functional.template;

import static io.harness.generator.TemplateFolderGenerator.TemplateFolders.TEMPLATE_FOLDER;
import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.TEXT;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.delegates.beans.ScriptType;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.AccountGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.TemplateFolderGenerator;
import io.harness.generator.TemplateGalleryGenerator;
import io.harness.generator.TemplateGenerator;
import io.harness.generator.WorkflowGenerator;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.BuildWorkflow;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.PhaseStep;
import software.wings.beans.RestResponse;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.service.impl.WorkflowExecutionServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.ws.rs.core.GenericType;

//@Ignore
public class TemplateFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private TemplateGalleryGenerator templateGalleryGenerator;
  @Inject private TemplateFolderGenerator templateFolderGenerator;
  @Inject private TemplateGenerator templateGenerator;
  @Inject private WorkflowExecutionServiceImpl workflowExecutionService;

  Workflow buildWorkflow;

  final Randomizer.Seed seed = new Randomizer.Seed(0);
  final String SCRIPT_TEMPLATE_NAME = "Sample Shell Script";
  OwnerManager.Owners owners;
  Application application;
  Account account;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = owners.obtainApplication(
        () -> applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST));
    account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    }
  }

  @Test
  @Category(FunctionalTests.class)
  @Ignore
  public void shouldExecuteShellScriptTemplateWorkflow() {
    GenericType<RestResponse<WorkflowExecution>> workflowExecutionType =
        new GenericType<RestResponse<WorkflowExecution>>() {

        };

    Template shellScriptTemplate =
        templateGenerator.ensurePredefined(seed, owners, TemplateGenerator.Templates.SHELL_SCRIPT);
    buildWorkflow = workflowGenerator.ensurePredefined(seed, owners, WorkflowGenerator.Workflows.BUILD_SHELL_SCRIPT);
    assertThat(buildWorkflow).isNotNull();

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setOrchestrationId(buildWorkflow.getUuid());
    executionArgs.setWorkflowType(buildWorkflow.getWorkflowType());
    executionArgs.setExecutionCredential(aSSHExecutionCredential().withExecutionType(SSH).build());
    RestResponse<WorkflowExecution> workflowExecutionRestResponse = given()
                                                                        .auth()
                                                                        .oauth2(bearerToken)
                                                                        .queryParam("appId", application.getUuid())
                                                                        .body(executionArgs)
                                                                        .contentType(ContentType.JSON)
                                                                        .post("/executions")
                                                                        .as(workflowExecutionType.getType());

    WorkflowExecution workflowExecution = workflowExecutionRestResponse.getResource();
    assertThat(workflowExecution).isNotNull();
    assertThat(workflowExecution.getWorkflowId()).isEqualTo(buildWorkflow.getUuid());
    // cleanup - unlink template and delete template
    Map<String, WorkflowPhase> map = ((BuildWorkflow) buildWorkflow.getOrchestrationWorkflow()).getWorkflowPhaseIdMap();
    for (Entry<String, WorkflowPhase> entry : map.entrySet()) {
      WorkflowPhase phase = entry.getValue();
      if (phase.getName().equalsIgnoreCase("Phase 1")) {
        List<PhaseStep> phaseSteps = phase.getPhaseSteps();
        for (PhaseStep phaseStep : phaseSteps) {
          if (phaseStep.getName().equalsIgnoreCase("Collect Artifact")) {
            phaseStep.setSteps(asList());
            given()
                .auth()
                .oauth2(bearerToken)
                .queryParam("appId", application.getUuid())
                .pathParam("workflowId", buildWorkflow.getUuid())
                .pathParam("phaseId", phase.getUuid())
                .body(phase)
                .contentType(ContentType.JSON)
                .put("/workflows/{workflowId}/phases/{phaseId}");
            break;
          }
        }
      }
    }

    //    workflowExecutionService.triggerEnvExecution(
    //        buildWorkflow.getAppId(), buildWorkflow.getEnvId(), executionArgs, null);

    // Delete template
    given()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", account.getUuid())
        .pathParam("templateId", shellScriptTemplate.getUuid())
        .delete("/templates/{templateId}")
        .then()
        .statusCode(200);
  }

  @Test
  @Category(FunctionalTests.class)
  @Ignore
  public void createUpdateDeleteShellScriptTemplate() {
    // Create template
    TemplateFolder parentFolder = templateFolderGenerator.ensurePredefined(seed, owners, TEMPLATE_FOLDER);
    ShellScriptTemplate shellScriptTemplate = ShellScriptTemplate.builder()
                                                  .scriptType(ScriptType.BASH.name())
                                                  .scriptString("echo \"Hello\" ${name}\n"
                                                      + "export A=\"aaa\"\n"
                                                      + "export B=\"bbb\"")
                                                  .outputVars("A,B")
                                                  .build();
    Template template = Template.builder()
                            .type(TemplateType.SHELL_SCRIPT.name())
                            .accountId(account.getUuid())
                            .name(SCRIPT_TEMPLATE_NAME)
                            .templateObject(shellScriptTemplate)
                            .folderId(parentFolder.getUuid())
                            .appId(GLOBAL_APP_ID)
                            .variables(asList(aVariable().withType(TEXT).withName("name").withMandatory(true).build()))
                            .build();
    GenericType<RestResponse<Template>> templateType = new GenericType<RestResponse<Template>>() {

    };

    RestResponse<Template> savedTemplateResponse = given()
                                                       .auth()
                                                       .oauth2(bearerToken)
                                                       .queryParam("accountId", account.getUuid())
                                                       .body(template)
                                                       .contentType(ContentType.JSON)
                                                       .post("/templates")
                                                       .as(templateType.getType());

    Template savedTemplate = savedTemplateResponse.getResource();
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getUuid()).isNotEmpty();
    assertThat(savedTemplate.getName()).isEqualTo(SCRIPT_TEMPLATE_NAME);
    assertThat(savedTemplate.getType()).isEqualTo(TemplateType.SHELL_SCRIPT.name());
    assertThat(savedTemplate.getVersion()).isEqualTo(1L);

    // Get template and validate template object and variables
    savedTemplateResponse = given()
                                .auth()
                                .oauth2(bearerToken)
                                .contentType(ContentType.JSON)
                                .queryParam("accountId", account.getUuid())
                                .pathParam("templateId", savedTemplate.getUuid())
                                .get("/templates/{templateId}")
                                .as(templateType.getType());

    savedTemplate = savedTemplateResponse.getResource();
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getUuid()).isNotEmpty();
    assertThat(savedTemplate.getName()).isEqualTo(SCRIPT_TEMPLATE_NAME);
    assertThat(savedTemplate.getType()).isEqualTo(TemplateType.SHELL_SCRIPT.name());
    assertThat(savedTemplate.getVersion()).isEqualTo(1L);
    assertThat(savedTemplate.getTemplateObject()).isNotNull();
    shellScriptTemplate = (ShellScriptTemplate) savedTemplate.getTemplateObject();
    assertThat(shellScriptTemplate.getOutputVars()).isEqualTo("A,B");
    assertThat(shellScriptTemplate.getTimeoutMillis()).isEqualTo(600000);
    assertThat(shellScriptTemplate.getScriptString())
        .isEqualTo("echo \"Hello\" ${name}\n"
            + "export A=\"aaa\"\n"
            + "export B=\"bbb\"");

    // update template and validate
    shellScriptTemplate = ShellScriptTemplate.builder()
                              .scriptType(ScriptType.BASH.name())
                              .scriptString("echo \"Hello\" ${name}\n"
                                  + "export A=\"aaa\"\n"
                                  + "export B=\"bbb\"\n"
                                  + "export C=\"ccc\"")
                              .outputVars("A,B,C")
                              .build();
    template = Template.builder()
                   .type(TemplateType.SHELL_SCRIPT.name())
                   .accountId(account.getUuid())
                   .name(SCRIPT_TEMPLATE_NAME)
                   .templateObject(shellScriptTemplate)
                   .folderId(parentFolder.getUuid())
                   .appId(GLOBAL_APP_ID)
                   .variables(asList(aVariable().withType(TEXT).withName("name").withMandatory(true).build()))
                   .version(savedTemplate.getVersion())
                   .build();
    savedTemplateResponse = given()
                                .auth()
                                .oauth2(bearerToken)
                                .contentType(ContentType.JSON)
                                .body(template)
                                .queryParam("accountId", account.getUuid())
                                .pathParam("templateId", savedTemplate.getUuid())
                                .put("/templates/{templateId}")
                                .as(templateType.getType());
    savedTemplate = savedTemplateResponse.getResource();
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getUuid()).isNotEmpty();
    assertThat(savedTemplate.getName()).isEqualTo(SCRIPT_TEMPLATE_NAME);
    assertThat(savedTemplate.getType()).isEqualTo(TemplateType.SHELL_SCRIPT.name());
    assertThat(savedTemplate.getVersion()).isEqualTo(2L);

    assertThat(savedTemplate.getTemplateObject()).isNotNull();
    shellScriptTemplate = (ShellScriptTemplate) savedTemplate.getTemplateObject();
    assertThat(shellScriptTemplate.getOutputVars()).isEqualTo("A,B,C");
    assertThat(shellScriptTemplate.getTimeoutMillis()).isEqualTo(600000);
    assertThat(shellScriptTemplate.getScriptString())
        .isEqualTo("echo \"Hello\" ${name}\n"
            + "export A=\"aaa\"\n"
            + "export B=\"bbb\"\n"
            + "export C=\"ccc\"");

    // Delete template
    given()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", account.getUuid())
        .pathParam("templateId", savedTemplate.getUuid())
        .delete("/templates/{templateId}")
        .then()
        .statusCode(200);

    // Make sure that it is deleted
    savedTemplateResponse = given()
                                .auth()
                                .oauth2(bearerToken)
                                .queryParam("accountId", account.getUuid())
                                .queryParam("version", "1")
                                .pathParam("templateId", savedTemplate.getUuid())
                                .get("/templates/{templateId}")
                                .as(templateType.getType());
    assertThat(savedTemplateResponse.getResource()).isNull();
  }
}