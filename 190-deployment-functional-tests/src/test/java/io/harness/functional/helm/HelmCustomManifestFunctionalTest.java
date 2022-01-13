/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.helm;

import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.generator.ServiceGenerator.Services.CUSTOM_MANIFEST_HELM_MULTIPLE_OVERRIDE;
import static io.harness.generator.ServiceGenerator.Services.CUSTOM_MANIFEST_HELM_S3;
import static io.harness.generator.ServiceGenerator.Services.CUSTOM_MANIFEST_HELM_VALUE_OVERRIDE;
import static io.harness.rule.OwnerRule.ABOSII;

import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.CDFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.functional.utils.ApplicationManifestHelper;
import io.harness.functional.utils.HelmHelper;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.WorkflowService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class HelmCustomManifestFunctionalTest extends AbstractFunctionalTest {
  private static final String WORKFLOW_NAME = "Helm Custom Manifest Functional Test";
  private static final String VALUE_OVERRIDE_WORKFLOW_NAME = "Helm Custom Manifest Value Override Functional Test";
  private static final String VALUE_MULTIPLE_OVERRIDE_WORKFLOW_NAME =
      "Helm Custom Manifest Multiple Override Functional Test";
  private static final String CLEANUP_WORKFLOW_PREFIX = "Cleanup ";
  private static final String CLEANUP_STEP_NAME = "Cleanup release";

  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private WorkflowService workflowService;
  @Inject private ApplicationManifestHelper applicationManifestHelper;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private HelmHelper helmHelper;

  final Randomizer.Seed seed = new Randomizer.Seed(0);
  OwnerManager.Owners owners;
  Application application;
  Environment environment;
  Service helmS3Service;
  InfrastructureDefinition infrastructureDefinition;
  Workflow workflow;
  Workflow cleanupWorkflow;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
    owners = ownerManager.create();
    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    infrastructureDefinition = infrastructureDefinitionGenerator.ensurePredefined(
        seed, owners, InfrastructureDefinitionGenerator.InfrastructureDefinitions.GCP_HELM_CUSTOM_MANIFEST_TEST);
    log.info("Ensured Infra def");
    resetCache(owners.obtainAccount().getUuid());
    logManagerFeatureFlags(application.getAccountId());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void helmCustomManifest() {
    helmS3Service = serviceGenerator.ensurePredefined(seed, owners, CUSTOM_MANIFEST_HELM_S3);
    testHelmWorkflowExecution(helmS3Service, WORKFLOW_NAME + System.currentTimeMillis());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void helmValuesOverrideCustomManifest() {
    helmS3Service = serviceGenerator.ensurePredefined(seed, owners, CUSTOM_MANIFEST_HELM_VALUE_OVERRIDE);
    testHelmWorkflowExecution(helmS3Service, VALUE_OVERRIDE_WORKFLOW_NAME + System.currentTimeMillis());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(CDFunctionalTests.class)
  public void helmMultipleValuesOverridesCustomManifest() {
    helmS3Service = serviceGenerator.ensurePredefined(seed, owners, CUSTOM_MANIFEST_HELM_MULTIPLE_OVERRIDE);
    testHelmWorkflowExecution(helmS3Service, VALUE_MULTIPLE_OVERRIDE_WORKFLOW_NAME + System.currentTimeMillis());
  }

  @NotNull
  private ExecutionArgs getExecutionArgs(Workflow workflow, String serviceName) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setOrchestrationId(workflow.getUuid());
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    executionArgs.setWorkflowVariables(ImmutableMap.of("serviceName", serviceName));
    return executionArgs;
  }

  private void testHelmWorkflowExecution(Service service, String workflowName) {
    String releaseName = helmHelper.getReleaseName(service.getName());
    log.info("Added values.yaml to service");
    workflow = createHelmWorkflow(workflowName, releaseName, service, infrastructureDefinition);
    log.info("Workflow created");
    cleanupWorkflow = createHelmCleanupWorkflow(workflowName, releaseName, service, infrastructureDefinition);

    resetCache(owners.obtainAccount().getUuid());
    ExecutionArgs executionArgs = getExecutionArgs(workflow, service.getName());

    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, service.getAppId(), infrastructureDefinition.getEnvId(), executionArgs);

    //     Cleanup
    logStateExecutionInstanceErrors(runWorkflow(bearerToken, service.getAppId(), infrastructureDefinition.getEnvId(),
        getExecutionArgs(cleanupWorkflow, service.getName())));

    logStateExecutionInstanceErrors(workflowExecution);
    getFailedWorkflowExecutionLogs(workflowExecution);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  private Workflow createHelmWorkflow(
      String name, String releaseName, Service service, InfrastructureDefinition infraDefinition) {
    Workflow workflow =
        aWorkflow()
            .name(name)
            .appId(service.getAppId())
            .envId(infraDefinition.getEnvId())
            .infraDefinitionId(infraDefinition.getUuid())
            .serviceId(service.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withUserVariables(Arrays.asList(aVariable().name("serviceName").mandatory(false).build()))
                    .build())
            .build();

    Workflow savedWorkflow = createAndAssertWorkflow(workflow, application.getAccountId(), application.getUuid());
    return helmHelper.setupWorkflow(savedWorkflow, releaseName, service.getHelmVersion());
  }

  private Workflow createHelmCleanupWorkflow(
      String name, String releaseName, Service service, InfrastructureDefinition infraDefinition) {
    Workflow workflow = aWorkflow()
                            .name(CLEANUP_WORKFLOW_PREFIX + name)
                            .appId(service.getAppId())
                            .envId(infraDefinition.getEnvId())
                            .infraDefinitionId(infraDefinition.getUuid())
                            .serviceId(service.getUuid())
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aBasicOrchestrationWorkflow().build())
                            .build();

    Workflow savedWorkflow = createAndAssertWorkflow(workflow, application.getAccountId(), application.getUuid());
    String cleanupScript = helmHelper.getCleanupScript(service.getHelmVersion(), releaseName);
    GraphNode cleanupStep = helmHelper.createShellScriptNode(CLEANUP_STEP_NAME, cleanupScript);

    BasicOrchestrationWorkflow orchestrationWorkflow =
        (BasicOrchestrationWorkflow) savedWorkflow.getOrchestrationWorkflow();
    WorkflowPhase workflowPhase = orchestrationWorkflow.getWorkflowPhases().get(0);
    workflowPhase.getPhaseSteps().get(0).setSteps(singletonList(cleanupStep));

    return workflowService.updateWorkflow(savedWorkflow, false);
  }
}
