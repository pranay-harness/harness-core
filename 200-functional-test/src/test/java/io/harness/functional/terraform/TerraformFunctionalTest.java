/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.terraform;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.YOGESH;

import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.sm.StepType.TERRAFORM_APPLY;
import static software.wings.sm.StepType.TERRAFORM_DESTROY;
import static software.wings.sm.StepType.TERRAFORM_PROVISION;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.InfrastructureProvisionerGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.SettingGenerator;
import io.harness.generator.WorkflowGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TerraformFunctionalTest extends AbstractFunctionalTest {
  private static final String WORKFLOW_NAME_PREFIX = "Terraform workflow ";
  private static final String PROVISIONER_NAME_PREFIX = "Terraform infrastructure provisioner ";
  private static final String TF_PLAN_STEP_NAME = "Terraform Provision Plan";
  private static final String TF_APPLY_STEP_NAME = "Terraform Apply";
  private static final String SHELL_SCRIPT_STEP_NAME = "Shell Script";
  private static final String RESOURCE_PATH = "200-functional-test/src/test/resources/io/harness/functional/terraform";
  private static final String SCRIPT_NAME_APPLY = "CheckTerraformApplyPlanJsonScript";
  private static final String SCRIPT_NAME_DESTROY = "CheckTerraformDestroyPlanJsonScript";
  private static final String TF_PLAN_DESTROY_STEP_NAME = "Terraform Destroy Plan";
  private static final String TF_DESTROY_STEP_NAME = "Terraform Destroy";

  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WorkflowService workflowService;
  @Inject private InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;

  final Randomizer.Seed seed = new Randomizer.Seed(0);
  OwnerManager.Owners owners;
  Application application;
  SettingAttribute gitConnector;
  Environment environment;

  @Before
  public void setUp() {
    owners = ownerManager.create();

    gitConnector = settingGenerator.ensurePredefined(seed, owners, SettingGenerator.Settings.TERRAFORM_MAIN_GIT_REPO);
    assertThat(gitConnector).isNotNull();

    application = owners.obtainApplication(
        () -> applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST));
    assertThat(application).isNotNull();

    environment = owners.obtainEnvironment(
        () -> environmentGenerator.ensurePredefined(seed, owners, EnvironmentGenerator.Environments.GENERIC_TEST));
    assertThat(environment).isNotNull();
    logManagerFeatureFlags(application.getAccountId());
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void createTerraformPlanForApply() throws IOException {
    InfrastructureProvisioner terraformProvisioner = createTerraformProvisioner(true, false);

    Workflow workflow = createCanaryWorkflow(environment.getUuid());
    workflow = createAndAssertWorkflow(workflow);
    workflowService.updatePreDeployment(application.getAppId(), workflow.getUuid(),
        addPreDeploymentRunPlanOnlyPhaseStep(terraformProvisioner, false, false));

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow));
    logStateExecutionInstanceErrors(workflowExecution);
    getFailedWorkflowExecutionLogs(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void applyApprovedPlanThenDestroy() throws IOException {
    InfrastructureProvisioner terraformProvisioner = createTerraformProvisioner(true, false);

    Workflow workflow = createCanaryWorkflow(environment.getUuid());
    workflow = createAndAssertWorkflow(workflow);
    workflowService.updatePreDeployment(
        application.getAppId(), workflow.getUuid(), addPreDeploymentPhaseStep(terraformProvisioner, true, false));
    workflowService.updatePostDeployment(
        application.getAppId(), workflow.getUuid(), addPostDeploymentPhaseStep(terraformProvisioner, false));

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow));
    logStateExecutionInstanceErrors(workflowExecution);
    getFailedWorkflowExecutionLogs(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void createTerraformPlanForDestroy() throws IOException {
    InfrastructureProvisioner terraformProvisioner = createTerraformProvisioner(true, false);

    Workflow workflow = createCanaryWorkflow(environment.getUuid());
    workflow = createAndAssertWorkflow(workflow);
    workflowService.updatePostDeployment(
        application.getAppId(), workflow.getUuid(), addPostDeploymentRunPlanOnlyPhaseStep(terraformProvisioner, false));

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow));
    logStateExecutionInstanceErrors(workflowExecution);
    getFailedWorkflowExecutionLogs(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void workflowWithCommitId() throws IOException {
    InfrastructureProvisioner terraformProvisioner = createTerraformProvisioner(false, false);

    Workflow workflow = createCanaryWorkflow(environment.getUuid());
    workflow = createAndAssertWorkflow(workflow);
    workflowService.updatePreDeployment(
        application.getAppId(), workflow.getUuid(), addPreDeploymentPhaseStep(terraformProvisioner, true, false));
    workflowService.updatePostDeployment(
        application.getAppId(), workflow.getUuid(), addPostDeploymentPhaseStep(terraformProvisioner, false));

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow));
    logStateExecutionInstanceErrors(workflowExecution);
    getFailedWorkflowExecutionLogs(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void workflowWithEnvVars() throws IOException {
    InfrastructureProvisioner terraformProvisioner = createTerraformProvisioner(true, true);

    Workflow workflow = createCanaryWorkflow(environment.getUuid());
    workflow = createAndAssertWorkflow(workflow);
    workflowService.updatePreDeployment(
        application.getAppId(), workflow.getUuid(), addPreDeploymentPhaseStep(terraformProvisioner, true, true));
    workflowService.updatePostDeployment(
        application.getAppId(), workflow.getUuid(), addPostDeploymentPhaseStep(terraformProvisioner, true));

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), getExecutionArgs(workflow));
    logStateExecutionInstanceErrors(workflowExecution);
    getFailedWorkflowExecutionLogs(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  private Workflow createCanaryWorkflow(String envId) {
    return aWorkflow()
        .name(WORKFLOW_NAME_PREFIX + System.currentTimeMillis())
        .appId(application.getUuid())
        .envId(envId)
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow().build())
        .build();
  }

  private PhaseStep addPreDeploymentPhaseStep(InfrastructureProvisioner infrastructureProvisioner,
      boolean exportPlanToApplyStep, boolean addEnvVars) throws IOException {
    PhaseStep phaseStep =
        addPreDeploymentRunPlanOnlyPhaseStep(infrastructureProvisioner, exportPlanToApplyStep, addEnvVars);

    GraphNode applyStep = createApplyStep(infrastructureProvisioner);

    phaseStep.getSteps().add(applyStep);
    return phaseStep;
  }

  private PhaseStep addPreDeploymentRunPlanOnlyPhaseStep(InfrastructureProvisioner infrastructureProvisioner,
      boolean exportPlanToApplyStep, boolean addEnvVars) throws IOException {
    // Terraform Provision
    GraphNode provisionStep = createProvisionStep(infrastructureProvisioner, exportPlanToApplyStep, addEnvVars);

    // Shell Script
    GraphNode shellScript = createShellScriptStep(false);

    return aPhaseStep(PhaseStepType.PRE_DEPLOYMENT, "Pre-Deployment")
        .addStep(provisionStep)
        .addStep(shellScript)
        .build();
  }

  private GraphNode createProvisionStep(
      InfrastructureProvisioner infrastructureProvisioner, boolean exportPlanToApplyStep, boolean addEnvVars) {
    return GraphNode.builder()
        .id(generateUuid())
        .name(TF_PLAN_STEP_NAME)
        .type(TERRAFORM_PROVISION.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("name", TF_PLAN_STEP_NAME)
                        .put("provisionerId", infrastructureProvisioner.getUuid())
                        .put("inheritApprovedPlan", false)
                        .put("runPlanOnly", true)
                        .put("exportPlanToApplyStep", exportPlanToApplyStep)
                        .put("variables", getVars(addEnvVars))
                        .put("environmentVariables", addEnvVars ? getEnvVars() : emptyList())
                        .build())
        .build();
  }

  private GraphNode createApplyStep(InfrastructureProvisioner infrastructureProvisioner) {
    return GraphNode.builder()
        .id(generateUuid())
        .name(TF_APPLY_STEP_NAME)
        .type(TERRAFORM_APPLY.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("name", TF_APPLY_STEP_NAME)
                        .put("provisionerId", infrastructureProvisioner.getUuid())
                        .put("inheritApprovedPlan", true)
                        .put("runPlanOnly", false)
                        .put("exportPlanToApplyStep", false)
                        .build())
        .build();
  }

  private GraphNode createShellScriptStep(boolean isDestroy) throws IOException {
    String shellScript = readFileContent(isDestroy ? SCRIPT_NAME_DESTROY : SCRIPT_NAME_APPLY, RESOURCE_PATH);
    return GraphNode.builder()
        .id(generateUuid())
        .name(SHELL_SCRIPT_STEP_NAME)
        .type(StateType.SHELL_SCRIPT.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("scriptType", "BASH")
                        .put("scriptString", shellScript)
                        .put("timeoutMillis", 60000)
                        .put("executeOnDelegate", true)
                        .build())
        .build();
  }

  private GraphNode createDestroyStep(
      InfrastructureProvisioner infrastructureProvisioner, boolean runPlanOnly, boolean addEnvVars) {
    return GraphNode.builder()
        .id(generateUuid())
        .name(runPlanOnly ? TF_PLAN_DESTROY_STEP_NAME : TF_DESTROY_STEP_NAME)
        .type(TERRAFORM_DESTROY.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("name", runPlanOnly ? TF_PLAN_DESTROY_STEP_NAME : TF_DESTROY_STEP_NAME)
                        .put("provisionerId", infrastructureProvisioner.getUuid())
                        .put("runPlanOnly", runPlanOnly)
                        .put("inheritApprovedPlan", !runPlanOnly)
                        .put("variables", runPlanOnly ? getVars(addEnvVars) : emptyList())
                        .put("environmentVariables", addEnvVars ? getEnvVars() : emptyList())
                        .build())
        .build();
  }

  private PhaseStep addPostDeploymentPhaseStep(InfrastructureProvisioner infrastructureProvisioner, boolean addEnvVars)
      throws IOException {
    PhaseStep phaseStep = addPostDeploymentRunPlanOnlyPhaseStep(infrastructureProvisioner, addEnvVars);

    GraphNode destroy = createDestroyStep(infrastructureProvisioner, false, false);
    phaseStep.getSteps().add(destroy);

    return phaseStep;
  }

  private PhaseStep addPostDeploymentRunPlanOnlyPhaseStep(
      InfrastructureProvisioner infrastructureProvisioner, boolean addEnvVars) throws IOException {
    GraphNode destroyPlan = createDestroyStep(infrastructureProvisioner, true, addEnvVars);
    GraphNode shellScript = createShellScriptStep(true);

    return aPhaseStep(PhaseStepType.POST_DEPLOYMENT, "Post Deployment")
        .addStep(destroyPlan)
        .addStep(shellScript)
        .build();
  }

  private InfrastructureProvisioner createTerraformProvisioner(boolean useBranch, boolean addEnvVars) {
    TerraformInfrastructureProvisioner terraformInfrastructureProvisioner =
        TerraformInfrastructureProvisioner.builder()
            .appId(application.getAppId())
            .name(PROVISIONER_NAME_PREFIX + System.currentTimeMillis())
            .sourceRepoSettingId(gitConnector.getUuid())
            .sourceRepoBranch(useBranch ? "functional_tests" : null)
            .commitId(useBranch ? null : "77ee4daae418d8f33808894a4185404190e29993")
            .skipRefreshBeforeApplyingPlan(true)
            .path("/localFile/")
            .variables(getVariables(addEnvVars))
            .environmentVariables(addEnvVars ? getEnvironmentVariables() : null)
            .mappingBlueprints(Arrays.asList())
            .kmsId(application.getAccountId())
            .build();

    return infrastructureProvisionerGenerator.ensureInfrastructureProvisioner(
        seed, owners, terraformInfrastructureProvisioner);
  }

  private List<NameValuePair> getVariables(boolean addEnvVars) {
    List<NameValuePair> variables = new ArrayList<>();
    if (!addEnvVars) {
      variables.add(NameValuePair.builder().name("var1").valueType("TEXT").build());
    }
    variables.add(NameValuePair.builder().name("fileName").valueType("TEXT").build());
    return variables;
  }

  private List<NameValuePair> getEnvironmentVariables() {
    List<NameValuePair> variables = new ArrayList<>();
    variables.add(NameValuePair.builder().name("TF_VAR_var1").valueType("TEXT").build());
    return variables;
  }

  private List<Map<String, String>> getVars(boolean addEnvVars) {
    List<Map<String, String>> variables = new ArrayList<>();
    if (!addEnvVars) {
      variables.add(ImmutableMap.of("name", "var1", "value", "var1value", "valueType", "TEXT"));
    }
    variables.add(ImmutableMap.of("name", "fileName", "value", "test_file", "valueType", "TEXT"));
    return variables;
  }

  private List<Map<String, String>> getEnvVars() {
    List<Map<String, String>> variables = new ArrayList<>();
    variables.add(ImmutableMap.of("name", "TF_VAR_var1", "value", "var1value", "valueType", "TEXT"));
    return variables;
  }

  public static String readFileContent(String filePath, String resourcePath) throws IOException {
    File scriptFile = null;
    scriptFile = new File(resourcePath + PATH_DELIMITER + filePath);

    return FileUtils.readFileToString(scriptFile, "UTF-8");
  }

  private ExecutionArgs getExecutionArgs(Workflow workflow) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    executionArgs.setOrchestrationId(workflow.getUuid());
    return executionArgs;
  }

  private Workflow createAndAssertWorkflow(Workflow workflow) {
    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), workflow);
    assertThat(savedWorkflow).isNotNull();
    assertThat(savedWorkflow.getUuid()).isNotEmpty();
    assertThat(savedWorkflow.getWorkflowType()).isEqualTo(ORCHESTRATION);
    return savedWorkflow;
  }
}
