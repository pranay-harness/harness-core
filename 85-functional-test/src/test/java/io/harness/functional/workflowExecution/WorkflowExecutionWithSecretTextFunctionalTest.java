package io.harness.functional.workflowExecution;

import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.rule.OwnerRule.DINESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.utils.SecretsUtils;
import io.harness.testframework.restutils.SecretsRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.Collections;

public class WorkflowExecutionWithSecretTextFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WorkflowUtils workflowUtils;

  private Application application;

  private final Seed seed = new Seed(0);
  private Owners owners;
  private Environment environment;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();
  }

  @Test
  @Owner(developers = DINESH)
  @Category(FunctionalTests.class)
  public void shouldHaveAccessToApplicationScopedSecretTextExpressionInWorkflow() throws Exception {
    String secretName = "test_application_scoped_secret_" + System.currentTimeMillis();
    String secretValue = "application scoped secret";
    SecretText secretText = SecretsUtils.createSecretTextObjectWithUsageRestriction(
        secretName, secretValue, environment.getEnvironmentType().name());
    SecretsRestUtils.addSecretWithUsageRestrictions(application.getAccountId(), bearerToken, secretText);

    String script = "echo ${secrets.getValue(\"" + secretName + "\")}";
    Workflow workflow = workflowUtils.createWorkflowWithShellScriptCommand(
        "access-secrets-text-application-scoped", application.getAppId(), "BASH", script);

    WorkflowExecution workflowExecution = createAndExecuteWorkflow(workflow);
    workflowUtils.validateWorkflowStatus(workflowExecution, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = DINESH)
  @Category(FunctionalTests.class)
  public void shouldNotHaveAccessToAccountScopedSecretTextExpressionInWorkflow() throws Exception {
    String secretName = "test_account_scoped_secret_" + System.currentTimeMillis();
    String secretValue = "account scoped secret";
    SecretsRestUtils.addSecret(
        application.getAccountId(), bearerToken, SecretsUtils.createSecretTextObject(secretName, secretValue));

    String script = "echo ${secrets.getValue(\"" + secretName + "\")}";
    Workflow workflow = workflowUtils.createWorkflowWithShellScriptCommand(
        "access-secrets-text-account-scoped", application.getAppId(), "BASH", script);
    WorkflowExecution workflowExecution = createAndExecuteWorkflow(workflow);

    workflowUtils.validateWorkflowStatus(workflowExecution, ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = DINESH)
  @Category(FunctionalTests.class)
  public void shouldFailForNonExistingSecretTextExpressionInWorkflow() throws Exception {
    String secretName = "non existing secret";
    String script = "echo ${secrets.getValue(\"" + secretName + "\")}";
    Workflow workflow = workflowUtils.createWorkflowWithShellScriptCommand(
        "access-non-existing-secret", application.getAppId(), "BASH", script);
    WorkflowExecution workflowExecution = createAndExecuteWorkflow(workflow);

    workflowUtils.validateWorkflowStatus(workflowExecution, ExecutionStatus.FAILED);
  }

  private WorkflowExecution createAndExecuteWorkflow(Workflow workflow) {
    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), workflow);
    assertThat(savedWorkflow).isNotNull();

    return runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), savedWorkflow.getUuid(),
        Collections.<Artifact>emptyList());
  }
}