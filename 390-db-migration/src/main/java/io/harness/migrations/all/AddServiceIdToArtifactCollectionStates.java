/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddServiceIdToArtifactCollectionStates implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject private WorkflowService workflowService;

  @Override
  @SuppressWarnings("deprecation")
  public void migrate() {
    log.info("Migration Started - add service id to artifact collection states");

    List<String> accountIds = wingsPersistence.createQuery(Account.class, excludeAuthority)
                                  .asList()
                                  .stream()
                                  .map(Account::getUuid)
                                  .collect(Collectors.toList());
    for (String accountId : accountIds) {
      migrateAccount(accountId);
    }

    log.info("Migration Completed - add service id to artifact collection states");
  }

  private void migrateAccount(String accountId) {
    Map<String, String> artifactStreamIdToServiceId = new HashMap<>();
    try (HIterator<Service> services = new HIterator<>(
             wingsPersistence.createQuery(Service.class).filter(ServiceKeys.accountId, accountId).fetch())) {
      for (Service service : services) {
        List<String> artifactStreamIds = artifactStreamServiceBindingService.listArtifactStreamIds(service);
        if (isEmpty(artifactStreamIds)) {
          continue;
        }

        String serviceId = service.getUuid();
        for (String artifactStreamId : artifactStreamIds) {
          artifactStreamIdToServiceId.put(artifactStreamId, serviceId);
        }
      }
    }

    try (HIterator<Workflow> workflowHIterator = new HIterator<>(wingsPersistence.createQuery(Workflow.class)
                                                                     .filter(WorkflowKeys.accountId, accountId)
                                                                     .project(WorkflowKeys.uuid, true)
                                                                     .project(WorkflowKeys.appId, true)
                                                                     .fetch())) {
      for (Workflow initialWorkflow : workflowHIterator) {
        migrateWorkflow(initialWorkflow, artifactStreamIdToServiceId);
      }
    }
  }

  private void migrateWorkflow(Workflow initialWorkflow, Map<String, String> artifactStreamIdToServiceId) {
    // Read the workflow.
    Workflow workflow;
    try {
      workflow = workflowService.readWorkflow(initialWorkflow.getAppId(), initialWorkflow.getUuid());
    } catch (Exception e) {
      log.error("Migration Error - could not read workflow: [{}]", initialWorkflow.getUuid(), e);
      return;
    }
    // Skip if the workflow is not a BUILD workflow.
    if (workflow == null || workflow.getOrchestrationWorkflow() == null
        || OrchestrationWorkflowType.BUILD != workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType()) {
      return;
    }

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    // Skip if orchestration workflow has no workflow phases.
    if (isEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
      return;
    }

    for (WorkflowPhase workflowPhase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
      // Skip if workflow phase has no phase steps.
      if (isEmpty(workflowPhase.getPhaseSteps())) {
        continue;
      }

      // We are updating at the workflow phase level. The updated variable tracks if there is any artifact
      // collection step inside this workflow phase whose properties need to be updated with the service id.
      boolean updated = false;
      for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
        // Skip if phase step is not of type COLLECT_ARTIFACT or has no workflow steps.
        if (PhaseStepType.COLLECT_ARTIFACT != phaseStep.getPhaseStepType() || isEmpty(phaseStep.getSteps())) {
          continue;
        }

        for (GraphNode step : phaseStep.getSteps()) {
          // Skip if step is not of type ARTIFACT_COLLECTION or has no properties.
          if (!StateType.ARTIFACT_COLLECTION.name().equals(step.getType()) || isEmpty(step.getProperties())) {
            continue;
          }

          Map<String, Object> properties = step.getProperties();
          String artifactStreamId = (String) properties.getOrDefault("artifactStreamId", null);
          // Skip if properties doesn't contain an artifact stream id.
          if (artifactStreamId == null) {
            continue;
          }

          String serviceId = artifactStreamIdToServiceId.getOrDefault(artifactStreamId, null);
          if (serviceId == null) {
            // NOTE: zombie artifact stream
            continue;
          }

          String propertiesServiceId = (String) properties.getOrDefault("serviceId", null);
          // Skip is properties already contains the required serviceId.
          if (propertiesServiceId != null && propertiesServiceId.equals(serviceId)) {
            continue;
          }

          properties.put("serviceId", serviceId);
          updated = true;
        }
      }

      if (updated) {
        try {
          workflowService.updateWorkflowPhase(workflow.getAppId(), workflow.getUuid(), workflowPhase);
        } catch (Exception e) {
          log.error("Migration Error - could not migrate workflow: [{}]", workflow.getUuid(), e);
        }
      }
    }
  }
}
