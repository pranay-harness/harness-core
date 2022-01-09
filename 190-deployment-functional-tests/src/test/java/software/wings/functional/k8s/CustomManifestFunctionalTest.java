/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.functional.k8s;

import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions.K8S_CUSTOM_MANIFEST;
import static io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions.OPENSHIFT_CUSTOM_MANIFEST;
import static io.harness.generator.ServiceGenerator.Services.CUSTOM_MANFIEST_OPENSHIFT_PARAMS_OVERRIDES;
import static io.harness.generator.ServiceGenerator.Services.CUSTOM_MANIFEST_K8S_V2_ABSOLUTE_PATH_TEST;
import static io.harness.generator.ServiceGenerator.Services.CUSTOM_MANIFEST_K8S_V2_NO_VALUES_TEST;
import static io.harness.generator.ServiceGenerator.Services.CUSTOM_MANIFEST_K8S_V2_TEST;
import static io.harness.generator.ServiceGenerator.Services.CUSTOM_MANIFEST_K8S_V2_VALUES_OVERRIDES;
import static io.harness.generator.ServiceGenerator.Services.CUSTOM_MANIFEST_OPENSHIFT_TEST;
import static io.harness.generator.ServiceGenerator.Services.CUSTOM_MANIFEST_OPENSHIFT_TEST_ABSOLUTE_PATH_TEST;
import static io.harness.generator.WorkflowGenerator.Workflows.K8S_ROLLING;
import static io.harness.rule.OwnerRule.ABOSII;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.CDFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.utils.ApplicationManifestHelper;
import io.harness.functional.utils.K8SUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.WorkflowGenerator;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class CustomManifestFunctionalTest extends AbstractFunctionalTest {
  private static final String CUSTOM_VALUES_OVERRIDES_SCRIPT = "k8s/custom-values-overrides-script.sh";
  private static final String CUSTOM_PARAMS_OVERRIDES_SCRIPT = "k8s/custom-params-overrides-script.sh";
  private static final String ENV_VALUES_OVERRIDES_PATH = "${serviceVariable.overridesPath}/values3.yaml,"
      + "${serviceVariable.overridesPath}/values4.yaml";
  private static final String ENV_VALUES_OVERRIDES_ABS_PATH =
      "${serviceVariable.absolutePath}/${serviceVariable.overridesPath}/values3.yaml, "
      + "${serviceVariable.absolutePath}/${serviceVariable.overridesPath}/values4.yaml";
  private static final String ENV_PARAMS_OVERRIDES_PATH = "${serviceVariable.overridesPath}/params3, "
      + "${serviceVariable.overridesPath}/params4";

  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureDefinitionGenerator infraDefinitionGenerator;
  @Inject private WorkflowService workflowService;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private ApplicationManifestHelper applicationManifestHelper;

  final Randomizer.Seed seed = new Randomizer.Seed(0);
  OwnerManager.Owners owners;
  Application application;
  Environment environment;
  Service service;
  InfrastructureDefinition infraDefinition;
  Workflow workflow;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST);
    assertThat(application).isNotNull();

    logManagerFeatureFlags(application.getAccountId());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void k8sCustomManifest() {
    prepareTest(CUSTOM_MANIFEST_K8S_V2_TEST, K8S_CUSTOM_MANIFEST);

    ExecutionArgs executionArgs = createExecutionArgs("k8s-custom-manifest", "default-override");
    WorkflowExecution workflowExecution = runTest(executionArgs);

    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void k8sAbsolutePathCustomManifest() {
    prepareTest(CUSTOM_MANIFEST_K8S_V2_ABSOLUTE_PATH_TEST, K8S_CUSTOM_MANIFEST);

    applicationManifestHelper.createCustomManifest(
        service, environment, CUSTOM_VALUES_OVERRIDES_SCRIPT, ENV_VALUES_OVERRIDES_ABS_PATH, AppManifestKind.VALUES);

    ExecutionArgs executionArgs = createExecutionArgs("k8s-abs-path-custom-manifest", "default-override",
        "default-override", "default-override", "values3-override", "values4-override");

    WorkflowExecution workflowExecution = runTest(executionArgs);
    // Delete created application manifest to not interfere with other tests
    applicationManifestHelper.cleanup(service, environment, AppManifestKind.VALUES);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void k8sNoDefaultValuesCustomManifest() {
    prepareTest(CUSTOM_MANIFEST_K8S_V2_NO_VALUES_TEST, K8S_CUSTOM_MANIFEST);

    ExecutionArgs executionArgs = createExecutionArgs("no-values-custom-manifest", "default-override");
    WorkflowExecution workflowExecution = runTest(executionArgs);

    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void k8serviceValuesOverrideCustomManifest() {
    prepareTest(CUSTOM_MANIFEST_K8S_V2_VALUES_OVERRIDES, K8S_CUSTOM_MANIFEST);

    ExecutionArgs executionArgs = createExecutionArgs("service-values-custom-manifest", "default-override",
        "values1-override", "values2-override", "values2-override", "default-override");
    WorkflowExecution workflowExecution = runTest(executionArgs);

    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void k8sMultipleValuesOverridesCustomManifest() {
    prepareTest(CUSTOM_MANIFEST_K8S_V2_VALUES_OVERRIDES, K8S_CUSTOM_MANIFEST);

    applicationManifestHelper.createCustomManifest(
        service, environment, CUSTOM_VALUES_OVERRIDES_SCRIPT, ENV_VALUES_OVERRIDES_PATH, AppManifestKind.VALUES);

    ExecutionArgs executionArgs = createExecutionArgs("values-overrides-custom-manifest", "default-override",
        "values1-override", "values2-override", "values3-override", "values4-override");
    WorkflowExecution workflowExecution = runTest(executionArgs);
    // Delete created application manifest to not interfere with other tests
    applicationManifestHelper.cleanup(service, environment, AppManifestKind.VALUES);

    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void openshiftCustomManifest() {
    prepareTest(CUSTOM_MANIFEST_OPENSHIFT_TEST, OPENSHIFT_CUSTOM_MANIFEST);

    ExecutionArgs executionArgs = createExecutionArgs("openshift-sample-custom-manifest", "default-override",
        "default-override", "default-override", "default-override", "default-override");
    WorkflowExecution workflowExecution = runTest(executionArgs);

    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void openshiftAbsolutePathCustomManifest() {
    prepareTest(CUSTOM_MANIFEST_OPENSHIFT_TEST_ABSOLUTE_PATH_TEST, OPENSHIFT_CUSTOM_MANIFEST);

    applicationManifestHelper.createCustomManifest(
        service, environment, CUSTOM_PARAMS_OVERRIDES_SCRIPT, ENV_PARAMS_OVERRIDES_PATH, AppManifestKind.OC_PARAMS);

    ExecutionArgs executionArgs = createExecutionArgs("oc-abs-path-custom-manifest", "default-override",
        "default-override", "default-override", "values3-override", "values4-override");
    WorkflowExecution workflowExecution = runTest(executionArgs);
    // Delete created application manifest to not interfere with other tests
    applicationManifestHelper.cleanup(service, environment, AppManifestKind.OC_PARAMS);

    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void openshiftServiceParamsOverrideCustomManifest() {
    prepareTest(CUSTOM_MANFIEST_OPENSHIFT_PARAMS_OVERRIDES, OPENSHIFT_CUSTOM_MANIFEST);

    ExecutionArgs executionArgs = createExecutionArgs("oc-service-params-custom-manifest", "default-override",
        "values1-override", "values2-override", "values2-override", "default-override");
    WorkflowExecution workflowExecution = runTest(executionArgs);

    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void openshiftMultipleParamsOverrideCustomManifest() {
    prepareTest(CUSTOM_MANFIEST_OPENSHIFT_PARAMS_OVERRIDES, OPENSHIFT_CUSTOM_MANIFEST);

    applicationManifestHelper.createCustomManifest(
        service, environment, CUSTOM_PARAMS_OVERRIDES_SCRIPT, ENV_PARAMS_OVERRIDES_PATH, AppManifestKind.OC_PARAMS);

    ExecutionArgs executionArgs = createExecutionArgs("oc-service-params-custom-manifest", "default-override",
        "values1-override", "values2-override", "values3-override", "values4-override");
    WorkflowExecution workflowExecution = runTest(executionArgs);
    // Delete created application manifest to not interfere with other tests
    applicationManifestHelper.cleanup(service, environment, AppManifestKind.OC_PARAMS);

    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @After
  public void cleanupRelease() {
    if (application == null || environment == null || service == null || infraDefinition == null || workflow == null) {
      log.warn("Nothing to cleanup or not all required entities has been initialized");
      return;
    }

    String workflowName = format("%16s", workflow.getName());
    Workflow existingCleanupWorkflow =
        workflowService.readWorkflowByName(application.getUuid(), format("Cleanup-%s", workflowName));
    if (existingCleanupWorkflow != null) {
      workflowService.deleteWorkflow(application.getUuid(), existingCleanupWorkflow.getUuid());
    }

    Workflow cleanupWorkflow = K8SUtils.createK8sCleanupWorkflow(application.getUuid(), environment.getUuid(),
        service.getUuid(), infraDefinition.getUuid(), workflowName, bearerToken, application.getAccountId());
    ExecutionArgs executionArgs = K8SUtils.createExecutionArgs(owners, cleanupWorkflow, bearerToken);
    runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);
  }

  private void prepareTest(Services serviceType, InfrastructureDefinitions infraDefType) {
    service = serviceGenerator.ensurePredefined(seed, owners, serviceType);
    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    infraDefinition = infraDefinitionGenerator.ensurePredefined(seed, owners, infraDefType);
    workflow = workflowGenerator.ensurePredefined(seed, owners, K8S_ROLLING);
  }

  private ExecutionArgs createExecutionArgs(String workloadName, String defaultOverride, String... overrides) {
    Map<String, String> workflowVariables = new HashMap<>();
    workflowVariables.put("workloadName", workloadName);
    workflowVariables.put("valueOverride", defaultOverride);

    for (int index = 0; index < overrides.length; index++) {
      workflowVariables.put("value" + (index + 1) + "Override", overrides[index]);
    }

    return K8SUtils.createExecutionArgs(owners, workflow, bearerToken, workflowVariables);
  }

  private WorkflowExecution runTest(ExecutionArgs executionArgs) {
    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);

    logStateExecutionInstanceErrors(workflowExecution);
    getFailedWorkflowExecutionLogs(workflowExecution);

    return workflowExecution;
  }
}
