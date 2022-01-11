/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.nas;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.PREPARE_STEPS;
import static software.wings.beans.PhaseStepType.WRAP_UP;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.AccountGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.SettingGenerator;
import io.harness.generator.WorkflowGenerator;
import io.harness.generator.artifactstream.ArtifactStreamManager;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.ArtifactStreamRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.api.ExecutionDataValue;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateType;
import software.wings.utils.RepositoryFormat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
@Slf4j
public class NASBuildWorkflowExecutionTest extends AbstractFunctionalTest {
  private static final String WRAP_UP_CONSTANT = "Wrap Up";
  @Inject private OwnerManager ownerManager;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private AccountGenerator accountGenerator;
  @Inject private ArtifactStreamManager artifactStreamManager;
  @Inject private SettingGenerator settingGenerator;

  private final Randomizer.Seed seed = new Randomizer.Seed(0);

  private OwnerManager.Owners owners;
  private Service service;
  Application application;
  Account account;
  SettingAttribute settingAttribute;
  Workflow workflow;
  ArtifactStream artifactStream;

  // Unique name of the workflow is ensured here
  public Workflow createBuildWorkflow(@NotEmpty String name, String appId, @NotEmpty String artifactStreamId) {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    List<GraphNode> steps = new ArrayList<>();
    Map<String, Object> runtimeValues = new HashMap<>();
    runtimeValues.put("repo", "releases");
    runtimeValues.put("groupId", "mygroup");
    runtimeValues.put("artifactId", "todolist");
    steps.add(GraphNode.builder()
                  .name("collect-artifact-" + artifactStreamId)
                  .type(StateType.ARTIFACT_COLLECTION.toString())
                  .properties(ImmutableMap.<String, Object>builder()
                                  .put("artifactStreamId", artifactStreamId)
                                  .put("runtimeValues", runtimeValues)
                                  .put("buildNo", "1.0")
                                  .put("entityType", "SERVICE")
                                  .build())
                  .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.PREPARE_STEPS, PREPARE_STEPS.toString()).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.COLLECT_ARTIFACT, PhaseStepType.COLLECT_ARTIFACT.toString())
                       .addAllSteps(steps)
                       .build());
    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());

    Workflow buildWorkflow =
        aWorkflow()
            .name(name)
            .appId(appId)
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aBuildOrchestrationWorkflow()
                                       .withWorkflowPhases(asList(aWorkflowPhase().phaseSteps(phaseSteps).build()))
                                       .build())
            .build();
    return buildWorkflow;
  }

  @Before
  public void setUp() {
    owners = ownerManager.create();

    application =
        applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.FUNCTIONAL_TEST);
    assertThat(application).isNotNull();

    service = serviceGenerator.ensurePredefined(seed, owners, ServiceGenerator.Services.NAS_FUNCTIONAL_TEST);
    assertThat(service).isNotNull();

    account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    }
    settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, SettingGenerator.Settings.HARNESS_NEXUS2_CONNECTOR);
  }

  @Test
  @Owner(developers = PRABU)
  @Category({FunctionalTests.class})
  public void executeBuildWorkflowWithParameterizedArtifactStream() {
    final String appId = service.getAppId();
    final String accountId = service.getAccountId();

    resetCache(accountId);
    // create parameterized nexus maven artifact stream
    artifactStream = artifactStreamManager.ensurePredefined(seed, owners,
        ArtifactStreamManager.ArtifactStreams.NEXUS2_MAVEN_METADATA_ONLY_PARAMETERIZED,
        NexusArtifactStream.builder()
            .appId(application.getUuid())
            .serviceId(service.getUuid())
            .autoPopulate(false)
            .metadataOnly(true)
            .name("nexus2-maven-metadataOnly-parameterized")
            .sourceName(settingAttribute.getName())
            .repositoryFormat(RepositoryFormat.maven.name())
            .jobname("${repo}")
            .groupId("${groupId}")
            .artifactPaths(asList("${artifactId}"))
            .settingId(settingAttribute.getUuid())
            .build());
    assertThat(artifactStream).isNotNull();
    service.setArtifactStreamIds(new ArrayList<>(Arrays.asList(artifactStream.getUuid())));
    log.info("Artifact stream " + artifactStream.getName()
        + " created successfully in service with name : " + service.getName());
    resetCache(accountId);
    //    workflow = workflowGenerator.ensureWorkflow(seed, owners,
    //        createBuildWorkflow("build-workflow-" + System.currentTimeMillis(), appId, artifactStream.getUuid()));
    //    resetCache(accountId);
    workflow = WorkflowRestUtils.createWorkflow(bearerToken, accountId, appId,
        createBuildWorkflow("build-workflow-" + System.currentTimeMillis(), appId, artifactStream.getUuid()));
    log.info("Workflow created successfully with name : " + workflow.getName());
    // Test running the workflow
    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, appId, null, workflow.getUuid(), Collections.emptyList());
    if (workflowExecution.getStatus() == ExecutionStatus.FAILED) {
      printErrorMessageFromNode(appId, workflowExecution);
    }
    log.info("Workflow execution status : " + workflowExecution.getStatus());
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  private void printErrorMessageFromNode(String appId, WorkflowExecution workflowExecution) {
    WorkflowExecution workflowExecutionWithDetails =
        workflowExecutionService.getExecutionDetails(appId, workflowExecution.getUuid(), true, false);
    GraphNode phase1 = workflowExecutionWithDetails.getExecutionNode().getNext();
    if (phase1 != null && phase1.getGroup() != null) {
      GraphNode prepareSteps = phase1.getGroup().getElements().get(0);
      GraphNode collectArtifact = prepareSteps.getNext();
      if (collectArtifact != null && collectArtifact.getGroup() != null) {
        GraphNode collectArtifactStep = collectArtifact.getGroup().getElements().get(0);
        Map<String, ExecutionDataValue> properties =
            (Map<String, ExecutionDataValue>) collectArtifactStep.getExecutionDetails();
        log.info("Workflow execution failed due to: " + properties.get("errorMsg").getValue());
      }
    }
  }

  @After
  public void tearDown() {
    WorkflowRestUtils.deleteWorkflow(bearerToken, workflow.getUuid(), application.getUuid());
    ArtifactStreamRestUtils.deleteArtifactStream(bearerToken, artifactStream.getUuid(), application.getUuid());
  }
}
