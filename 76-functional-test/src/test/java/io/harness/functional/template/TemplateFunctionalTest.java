package io.harness.functional.template;

import static io.harness.generator.AccountGenerator.adminUserEmail;
import static io.harness.generator.AccountGenerator.readOnlyEmail;
import static io.harness.generator.TemplateFolderGenerator.TemplateFolders.TEMPLATE_FOLDER;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.TEXT;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.FunctionalTests;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.AccountGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.TemplateFolderGenerator;
import io.harness.generator.WorkflowGenerator;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.ShellScriptTemplate;

import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.GenericType;

public class TemplateFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private TemplateFolderGenerator templateFolderGenerator;

  Workflow buildWorkflow;

  final Randomizer.Seed seed = new Randomizer.Seed(0);
  final String SCRIPT_TEMPLATE_NAME = "Another Sample Shell Script";
  final String SCRIPT_NAME1 = "Another Sample Shell Script - 1";
  final String SCRIPT_NAME2 = "Another Sample Shell Script - 2";
  final String SCRIPT_NAME3 = "Another Sample Shell Script - 3";
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
  public void shouldExecuteShellScriptTemplateWorkflow() {
    GenericType<RestResponse<WorkflowExecution>> workflowExecutionType =
        new GenericType<RestResponse<WorkflowExecution>>() {

        };

    buildWorkflow = workflowGenerator.ensurePredefined(seed, owners, WorkflowGenerator.Workflows.BUILD_SHELL_SCRIPT);

    resetCache();
    assertThat(buildWorkflow).isNotNull();

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setOrchestrationId(buildWorkflow.getUuid());
    executionArgs.setWorkflowType(buildWorkflow.getWorkflowType());
    executionArgs.setExecutionCredential(aSSHExecutionCredential().withExecutionType(SSH).build());
    RestResponse<WorkflowExecution> workflowExecutionRestResponse = Setup.portal()
                                                                        .auth()
                                                                        .oauth2(bearerToken)
                                                                        .queryParam("appId", application.getUuid())
                                                                        .body(executionArgs)
                                                                        .contentType(ContentType.JSON)
                                                                        .post("/executions")
                                                                        .as(workflowExecutionType.getType());

    WorkflowExecution workflowExecution = workflowExecutionRestResponse.getResource();
    assertThat(workflowExecution).isNotNull();

    // Poll for success status
    Awaitility.await()
        .atMost(120, TimeUnit.SECONDS)
        .pollInterval(5, TimeUnit.SECONDS)
        .until(()
                   -> Setup.portal()
                          .auth()
                          .oauth2(bearerToken)
                          .queryParam("appId", application.getUuid())
                          .get("/executions/" + workflowExecution.getUuid())
                          .jsonPath()
                          .<String>getJsonObject("resource.status")
                          .equals(ExecutionStatus.SUCCESS.name()));
  }

  @Test
  @Category(FunctionalTests.class)
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
                            .appId(Application.GLOBAL_APP_ID)
                            .variables(asList(aVariable().type(TEXT).name("name").mandatory(true).build()))
                            .build();
    GenericType<RestResponse<Template>> templateType = new GenericType<RestResponse<Template>>() {

    };

    RestResponse<Template> savedTemplateResponse = Setup.portal()
                                                       .auth()
                                                       .oauth2(bearerToken)
                                                       .queryParam("accountId", account.getUuid())
                                                       .body(template)
                                                       .contentType(ContentType.JSON)
                                                       .post("/templates")
                                                       .as(templateType.getType());

    Template savedTemplate = savedTemplateResponse.getResource();
    resetCache();
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getUuid()).isNotEmpty();
    assertThat(savedTemplate.getName()).isEqualTo(SCRIPT_TEMPLATE_NAME);
    assertThat(savedTemplate.getType()).isEqualTo(TemplateType.SHELL_SCRIPT.name());
    assertThat(savedTemplate.getVersion()).isEqualTo(1L);

    // Get template and validate template object and variables
    savedTemplateResponse = Setup.portal()
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
                   .appId(Application.GLOBAL_APP_ID)
                   .variables(asList(aVariable().type(TEXT).name("name").mandatory(true).build()))
                   .version(savedTemplate.getVersion())
                   .build();
    savedTemplateResponse = Setup.portal()
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
    deleteTemplate(bearerToken, account.getUuid(), savedTemplate.getUuid(), 200);

    // Make sure that it is deleted
    savedTemplateResponse = Setup.portal()
                                .auth()
                                .oauth2(bearerToken)
                                .queryParam("accountId", account.getUuid())
                                .queryParam("version", "1")
                                .pathParam("templateId", savedTemplate.getUuid())
                                .get("/templates/{templateId}")
                                .as(templateType.getType());
    assertThat(savedTemplateResponse.getResource()).isNull();
  }

  @Test
  @Category(FunctionalTests.class)
  public void testCRUDTemplateRBAC() {
    String readOnlyPassword = "readonlyuser";
    String bearerToken = Setup.getAuthToken(readOnlyEmail, readOnlyPassword);

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
                            .name(SCRIPT_NAME3)
                            .templateObject(shellScriptTemplate)
                            .folderId(parentFolder.getUuid())
                            .appId(Application.GLOBAL_APP_ID)
                            .variables(asList(aVariable().type(TEXT).name("name").mandatory(true).build()))
                            .build();
    GenericType<RestResponse<Template>> templateType = new GenericType<RestResponse<Template>>() {

    };

    RestResponse<Template> savedTemplateResponse = Setup.portal()
                                                       .auth()
                                                       .oauth2(bearerToken)
                                                       .queryParam("accountId", account.getUuid())
                                                       .body(template)
                                                       .contentType(ContentType.JSON)
                                                       .post("/templates")
                                                       .as(templateType.getType());
    assertThat(savedTemplateResponse.getResponseMessages()).isNotEmpty();
    assertThat(savedTemplateResponse.getResponseMessages().get(0).getCode().getStatus().getStatusCode()).isEqualTo(400);

    bearerToken = Setup.getAuthToken(adminUserEmail, "admin");
    savedTemplateResponse = Setup.portal()
                                .auth()
                                .oauth2(bearerToken)
                                .queryParam("accountId", account.getUuid())
                                .body(template)
                                .contentType(ContentType.JSON)
                                .post("/templates")
                                .as(templateType.getType());

    Template savedTemplate = savedTemplateResponse.getResource();
    resetCache();
    assertThat(savedTemplate).isNotNull();

    // Get template and validate template object and variables
    savedTemplateResponse = Setup.portal()
                                .auth()
                                .oauth2(bearerToken)
                                .contentType(ContentType.JSON)
                                .queryParam("accountId", account.getUuid())
                                .pathParam("templateId", savedTemplate.getUuid())
                                .get("/templates/{templateId}")
                                .as(templateType.getType());

    savedTemplate = savedTemplateResponse.getResource();
    assertThat(savedTemplate).isNotNull();

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
                   .name(SCRIPT_NAME3)
                   .templateObject(shellScriptTemplate)
                   .folderId(parentFolder.getUuid())
                   .appId(Application.GLOBAL_APP_ID)
                   .variables(asList(aVariable().type(TEXT).name("name").mandatory(true).build()))
                   .version(savedTemplate.getVersion())
                   .build();

    bearerToken = Setup.getAuthToken(readOnlyEmail, readOnlyPassword);

    savedTemplateResponse = Setup.portal()
                                .auth()
                                .oauth2(bearerToken)
                                .contentType(ContentType.JSON)
                                .body(template)
                                .queryParam("accountId", account.getUuid())
                                .pathParam("templateId", savedTemplate.getUuid())
                                .put("/templates/{templateId}")
                                .as(templateType.getType());

    assertThat(savedTemplateResponse.getResponseMessages()).isNotEmpty();
    assertThat(savedTemplateResponse.getResponseMessages().get(0).getCode().getStatus().getStatusCode()).isEqualTo(400);

    // Delete template shouldn't be allowed
    deleteTemplate(bearerToken, account.getUuid(), savedTemplate.getUuid(), 400);

    bearerToken = Setup.getAuthToken(adminUserEmail, "admin");

    // Delete template
    deleteTemplate(bearerToken, account.getUuid(), savedTemplate.getUuid(), 200);
  }

  private void deleteTemplate(String bearerToken, String accountId, String templateId, int expectedHttpResponse) {
    // Delete template
    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .pathParam("templateId", templateId)
        .delete("/templates/{templateId}")
        .then()
        .statusCode(expectedHttpResponse);
  }

  @Test
  @Category(FunctionalTests.class)
  public void shouldNotUpdateTemplateWithDuplicateNameInSameFolder() {
    GenericType<RestResponse<Template>> templateType = new GenericType<RestResponse<Template>>() {};
    Template template1 = createTemplateAndValidate(SCRIPT_NAME1);
    Template template2 = createTemplateAndValidate(SCRIPT_NAME2);
    template2.setName(SCRIPT_NAME1);
    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .contentType(ContentType.JSON)
        .body(template2)
        .queryParam("accountId", account.getUuid())
        .pathParam("templateId", template2.getUuid())
        .put("/templates/{templateId}")
        .as(templateType.getType());
    RestResponse<Template> savedTemplateResponse = Setup.portal()
                                                       .auth()
                                                       .oauth2(bearerToken)
                                                       .contentType(ContentType.JSON)
                                                       .queryParam("accountId", account.getUuid())
                                                       .pathParam("templateId", template2.getUuid())
                                                       .get("/templates/{templateId}")
                                                       .as(templateType.getType());
    assertThat(savedTemplateResponse.getResource().getName()).isEqualTo(SCRIPT_NAME2);
    deleteTemplate(template1.getUuid());
    deleteTemplate(template2.getUuid());
  }

  private Template createTemplateAndValidate(String name) {
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
                            .name(name)
                            .templateObject(shellScriptTemplate)
                            .folderId(parentFolder.getUuid())
                            .appId(Application.GLOBAL_APP_ID)
                            .variables(asList(aVariable().type(TEXT).name("name").mandatory(true).build()))
                            .build();
    GenericType<RestResponse<Template>> templateType = new GenericType<RestResponse<Template>>() {

    };

    RestResponse<Template> savedTemplateResponse = Setup.portal()
                                                       .auth()
                                                       .oauth2(bearerToken)
                                                       .queryParam("accountId", account.getUuid())
                                                       .body(template)
                                                       .contentType(ContentType.JSON)
                                                       .post("/templates")
                                                       .as(templateType.getType());

    Template savedTemplate = savedTemplateResponse.getResource();
    resetCache();
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getUuid()).isNotEmpty();
    assertThat(savedTemplate.getName()).isEqualTo(name);
    assertThat(savedTemplate.getType()).isEqualTo(TemplateType.SHELL_SCRIPT.name());
    assertThat(savedTemplate.getVersion()).isEqualTo(1L);

    // Get template and validate template object and variables
    savedTemplateResponse = Setup.portal()
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
    assertThat(savedTemplate.getName()).isEqualTo(name);
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
    return savedTemplate;
  }

  private void deleteTemplate(String templateId) {
    // Delete template
    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", account.getUuid())
        .pathParam("templateId", templateId)
        .delete("/templates/{templateId}")
        .then()
        .statusCode(200);
  }
}