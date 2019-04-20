package io.harness.functional.provisioners.terraform;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.RestUtils.InfraProvisionerRestUtils;
import io.harness.RestUtils.WorkflowRestUtils;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.exception.WingsException;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.AccountGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureProvisionerGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SecretGenerator;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.generator.WorkflowGenerator;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateType;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TerraformTest extends AbstractFunctionalTest {
  private static final String secretKeyName = "aws_playground_secret_key";
  private static final long TEST_TIMEOUT_IN_MINUTES = 3;

  private final Seed seed = new Seed(0);
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private AccountGenerator accountGenerator;
  @Inject private InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;
  @Inject private SecretGenerator secretGenerator;
  @Inject private InfraProvisionerRestUtils infraProvisionerRestUtil;
  @Inject private WorkflowRestUtils workflowRestUtil;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ScmSecret scmSecret;

  private Application application;
  private Service service;
  private Environment environment;
  private TerraformInfrastructureProvisioner terraformInfrastructureProvisioner;
  private Workflow workflow;
  private SettingAttribute settingAttribute;
  private String secretKeyValue;

  private Owners owners;

  @Before
  public void setUp() throws Exception {
    owners = ownerManager.create();
    ensurePredefinedStuff();

    terraformInfrastructureProvisioner = buildProvisionerObject();
    terraformInfrastructureProvisioner = (TerraformInfrastructureProvisioner) infraProvisionerRestUtil.saveProvisioner(
        application.getAppId(), terraformInfrastructureProvisioner);

    workflow = buildWorkflow(terraformInfrastructureProvisioner);
    workflow = workflowRestUtil.createWorkflow(application.getAccountId(), application.getUuid(), workflow);
  }

  private void ensurePredefinedStuff() {
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.FUNCTIONAL_TEST);
    service = serviceGenerator.ensurePredefined(seed, owners, Services.FUNCTIONAL_TEST);
    environment = environmentGenerator.ensurePredefined(seed, owners, Environments.FUNCTIONAL_TEST);
    settingAttribute = settingGenerator.ensurePredefined(seed, owners, Settings.TERRAFORM_MAIN_GIT_REPO);
    secretKeyValue = secretGenerator.ensureStored(owners, SecretName.builder().value(secretKeyName).build());
  }

  @Test
  @Owner(emails = "vaibhav.si@harness.io", intermittent = true)
  @Category(FunctionalTests.class)
  @Ignore
  public void shouldRunTerraformWorkflow() {
    ExecutionArgs executionArgs = prepareExecutionArgs(workflow);
    WorkflowExecution workflowExecution = runWorkflow(application.getAppId(), environment.getUuid(), executionArgs);
    checkForWorkflowSuccess(workflowExecution);
  }

  private void checkForWorkflowSuccess(WorkflowExecution workflowExecution) {
    Awaitility.await().atMost(TEST_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      ExecutionStatus status =
          workflowExecutionService.getWorkflowExecution(application.getUuid(), workflowExecution.getUuid()).getStatus();
      return status == ExecutionStatus.SUCCESS || status == ExecutionStatus.FAILED;
    });
    WorkflowExecution finalWorkflowExecution =
        workflowExecutionService.getWorkflowExecution(application.getUuid(), workflowExecution.getUuid());
    if (finalWorkflowExecution.getStatus() != ExecutionStatus.SUCCESS) {
      throw new WingsException(
          "workflow execution did not succeed. Final status: " + finalWorkflowExecution.getStatus());
    }
  }

  private WorkflowExecution runWorkflow(String appId, String envId, ExecutionArgs executionArgs) {
    return workflowRestUtil.runWorkflow(appId, envId, executionArgs);
  }

  private ExecutionArgs prepareExecutionArgs(Workflow workflow) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    executionArgs.setOrchestrationId(workflow.getUuid());
    return executionArgs;
  }

  private Workflow buildWorkflow(TerraformInfrastructureProvisioner terraformInfrastructureProvisioner)
      throws Exception {
    return aWorkflow()
        .name("Terraform Test")
        .envId(environment.getUuid())
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(
                    aPhaseStep(PRE_DEPLOYMENT)
                        .addStep(buildTerraformProvisionStep(terraformInfrastructureProvisioner.getUuid()))
                        .build())
                .withPostDeploymentSteps(
                    aPhaseStep(POST_DEPLOYMENT)
                        .addStep(buildTerraformDestroyStep(terraformInfrastructureProvisioner.getUuid()))
                        .build())
                .build())
        .build();
  }

  private GraphNode buildTerraformDestroyStep(String provisionerId) {
    return GraphNode.builder()
        .id(generateUuid())
        .type(StateType.TERRAFORM_DESTROY.getType())
        .name("Terraform Destroy")
        .properties(ImmutableMap.<String, Object>builder().put("provisionerId", provisionerId).build())
        .build();
  }

  private GraphNode buildTerraformProvisionStep(String provisionerId) throws Exception {
    InfrastructureProvisioner provisioner =
        infraProvisionerRestUtil.getProvisioner(application.getUuid(), provisionerId);
    provisioner = setValuesToProvisioner(provisioner);
    return GraphNode.builder()
        .id(generateUuid())
        .type(StateType.TERRAFORM_PROVISION.getType())
        .name("Terraform Provision")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("provisionerId", provisionerId)
                        .put("variables", provisioner.getVariables())
                        .build())
        .build();
  }

  private InfrastructureProvisioner setValuesToProvisioner(InfrastructureProvisioner provisioner) throws Exception {
    for (NameValuePair variable : provisioner.getVariables()) {
      String value;
      switch (variable.getName()) {
        case "access_key":
          value = String.valueOf(
              scmSecret.decryptToString(SecretName.builder().value("aws_playground_access_key").build()));
          break;
        case "secret_key":
          value = secretKeyValue;
          break;
        default:
          throw new Exception("Unknown variable: " + variable.getName() + " in provisioner");
      }
      variable.setValue(value);
    }

    return provisioner;
  }

  private TerraformInfrastructureProvisioner buildProvisionerObject() {
    List<NameValuePair> variables =
        Arrays.asList(NameValuePair.builder().name("access_key").valueType(Type.TEXT.toString()).build(),
            NameValuePair.builder().name("secret_key").valueType(Type.ENCRYPTED_TEXT.toString()).build());
    return TerraformInfrastructureProvisioner.builder()
        .appId(application.getAppId())
        .name("Terraform Test")
        .sourceRepoSettingId(settingAttribute.getUuid())
        .sourceRepoBranch("master")
        .path("functionalTest/")
        .variables(variables)
        .build();
  }
}