package io.harness.functional.pcf;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.WorkflowGenerator;
import io.harness.rule.OwnerRule;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.restutils.ArtifactRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureType;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.FeatureFlagService;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class PcfFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private ApplicationGenerator applicationGenerator;

  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private FeatureFlagService featureFlagService;

  private final Seed seed = new Seed(0);
  private Owners owners;

  private Service service;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST));

    featureFlagService.enableAccount(FeatureName.PCF_MANIFEST_REDESIGN, owners.obtainAccount().getUuid());
    featureFlagService.enableAccount(FeatureName.USE_PCF_CLI, owners.obtainAccount().getUuid());
    featureFlagService.enableAccount(FeatureName.PCF_CUSTOM_PLUGIN_SUPPORT, owners.obtainAccount().getUuid());
    featureFlagService.enableAccount(FeatureName.INFRA_MAPPING_REFACTOR, owners.obtainAccount().getUuid());
    resetCache(owners.obtainAccount().getUuid());
  }

  @Test
  @Owner(developers = OwnerRule.PRASHANT)
  @Category(FunctionalTests.class)
  @Ignore("Need to figure out the time outs")
  public void shouldCreateAndRunPcfBasicWorkflow() {
    WorkflowExecution workflowExecution = createAndExecuteWorkflow();
    workflowUtils.checkForWorkflowSuccess(workflowExecution);
  }

  @Test
  @Owner(developers = OwnerRule.PRASHANT)
  @Category(FunctionalTests.class)
  @Ignore("Need to figure out the time outs")
  public void shouldCreateAndRunPcfBasicWorkflowAndRollback() {
    WorkflowExecution firstExecution = createAndExecuteWorkflow();
    workflowUtils.checkForWorkflowSuccess(firstExecution);
    WorkflowExecution secondExecution = createAndExecuteWorkflow();
    workflowUtils.checkForWorkflowSuccess(secondExecution);
    resetCache(this.service.getAccountId());
    WorkflowExecution rollbackExecution =
        WorkflowRestUtils.rollbackExecution(bearerToken, secondExecution.getAppId(), secondExecution.getUuid());
    workflowUtils.checkForWorkflowSuccess(rollbackExecution);
  }

  private WorkflowExecution createAndExecuteWorkflow() {
    service = serviceGenerator.ensurePredefined(seed, owners, Services.PCF_V2_TEST);
    resetCache(service.getAccountId());
    InfrastructureDefinition infrastructureDefinition = infrastructureDefinitionGenerator.ensurePredefined(
        seed, owners, InfrastructureType.PCF_INFRASTRUCTURE, bearerToken);
    resetCache(service.getAccountId());
    Workflow workflow = workflowUtils.createPcfWorkflow("pcf-wf", service, infrastructureDefinition);
    workflow = workflowGenerator.ensureWorkflow(seed, owners, workflow);
    Artifact artifact = getArtifact(service, service.getAppId());
    return executeWorkflow(workflow, service, Arrays.asList(artifact), ImmutableMap.<String, String>builder().build());
  }

  private WorkflowExecution executeWorkflow(final Workflow workflow, final Service service,
      final List<Artifact> artifacts, ImmutableMap<String, String> workflowVariables) {
    final String appId = service.getAppId();
    final String envId = workflow.getEnvId();

    resetCache(this.service.getAccountId());
    ExecutionArgs executionArgs = prepareExecutionArgs(workflow, artifacts, workflowVariables);
    return WorkflowRestUtils.startWorkflow(bearerToken, appId, envId, executionArgs);
  }

  private Artifact getArtifact(Service service, String appId) {
    return ArtifactRestUtils.waitAndFetchArtifactByArtfactStream(
        bearerToken, appId, service.getArtifactStreamIds().get(0));
  }

  private ExecutionArgs prepareExecutionArgs(
      Workflow workflow, List<Artifact> artifacts, ImmutableMap<String, String> workflowFlowVariables) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    executionArgs.setOrchestrationId(workflow.getUuid());
    executionArgs.setArtifacts(artifacts);
    executionArgs.setWorkflowVariables(workflowFlowVariables);
    return executionArgs;
  }

  @After
  public void cleanUp() {}
}
