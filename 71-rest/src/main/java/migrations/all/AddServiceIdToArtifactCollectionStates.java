package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AddServiceIdToArtifactCollectionStates implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject private WorkflowService workflowService;

  @Override
  @SuppressWarnings("deprecation")
  public void migrate() {
    logger.info("Migration Started - add service id to artifact collection states");

    List<String> accountIds = wingsPersistence.createQuery(Account.class)
                                  .asList()
                                  .stream()
                                  .map(Account::getUuid)
                                  .collect(Collectors.toList());
    for (String accountId : accountIds) {
      migrateAccount(accountId);
    }

    logger.info("Migration Completed - add service id to artifact collection states");
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
        // Read the workflow.
        Workflow workflow = workflowService.readWorkflow(initialWorkflow.getAppId(), initialWorkflow.getUuid());
        // Skip if the workflow is not a BUILD workflow.
        if (workflow == null || workflow.getOrchestrationWorkflow() == null
            || !OrchestrationWorkflowType.BUILD.equals(
                   workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType())) {
          continue;
        }

        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
            (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
        // Skip if orchestration workflow has no workflow phases.
        if (isEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
          continue;
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
            if (!PhaseStepType.COLLECT_ARTIFACT.equals(phaseStep.getPhaseStepType()) || isEmpty(phaseStep.getSteps())) {
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
            workflowService.updateWorkflowPhase(workflow.getAppId(), workflow.getUuid(), workflowPhase);
          }
        }
      }
    }
  }
}
