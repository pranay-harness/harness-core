package software.wings.service.impl.trigger;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.trigger.Condition.Type.NEW_ARTIFACT;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Variable;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.Action;
import software.wings.beans.trigger.Action.ActionType;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.ArtifactTriggerCondition.ArtifactTriggerConditionKeys;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.DeploymentTrigger.DeploymentTriggerKeys;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.TriggerArtifactSelectionArtifact;
import software.wings.beans.trigger.TriggerArtifactSelectionPipeline;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.trigger.TriggerArtifactSelectionWorkflow;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.WorkflowService;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Singleton
@Slf4j
public class DeploymentTriggerServiceHelper {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceVariableService serviceVariablesService;
  @Inject private PipelineService pipelineService;
  @Inject private WorkflowService workflowService;
  @Inject private ArtifactStreamService artifactStreamService;

  public List<DeploymentTrigger> getNewArtifactTriggers(String appId, String artifactStreamId) {
    if (GLOBAL_APP_ID.equals(appId)) {
      return getNewArtifactTriggersForAllApps(artifactStreamId);
    }

    return getTriggersByApp(appId)
        .stream()
        .filter(tr
            -> tr.getCondition().getType().equals(NEW_ARTIFACT)
                && ((ArtifactCondition) tr.getCondition()).getArtifactStreamId().equals(artifactStreamId))
        .collect(toList());
  }

  public List<DeploymentTrigger> getTriggersByApp(String appId) {
    return wingsPersistence.query(DeploymentTrigger.class, aPageRequest().addFilter("appId", EQ, appId).build())
        .getResponse();
  }

  public boolean checkArtifactMatchesArtifactFilter(String triggerId, Artifact artifact, String artifactFilter) {
    Pattern pattern;
    try {
      pattern = compile(artifactFilter);
    } catch (PatternSyntaxException pe) {
      logger.warn("Invalid Build/Tag Filter {} for triggerId {}", artifactFilter, triggerId, pe);
      throw new WingsException("Invalid Build/Tag Filter", USER);
    }

    if (!isEmpty(artifact.getArtifactFiles())) {
      logger.info("Comparing artifact file name matches with the given artifact filter");
      List<ArtifactFile> artifactFiles = artifact.getArtifactFiles()
                                             .stream()
                                             .filter(artifactFile -> pattern.matcher(artifactFile.getName()).find())
                                             .collect(toList());
      if (isNotEmpty(artifactFiles)) {
        logger.info("Artifact file names matches with the given artifact filter");
        artifact.setArtifactFiles(artifactFiles);
        return true;
      }

    } else {
      if (pattern.matcher(artifact.getBuildNo()).find()) {
        logger.info(
            "Artifact filter {} matching with artifact name/ tag / buildNo {}", artifactFilter, artifact.getBuildNo());
        return true;
      }
    }
    return false;
  }

  public void validateTriggerAction(DeploymentTrigger trigger) {
    Action action = trigger.getAction();
    if (action.getActionType() == ActionType.PIPELINE) {
      PipelineAction pipelineAction = (PipelineAction) action;
      validateTriggerArgs(trigger.getAppId(), pipelineAction.getTriggerArgs());
    } else if (action.getActionType() == ActionType.ORCHESTRATION) {
      WorkflowAction workflowAction = (WorkflowAction) action;
      validateTriggerArgs(trigger.getAppId(), workflowAction.getTriggerArgs());
    }
  }

  private void validateTriggerArgs(String appId, TriggerArgs triggerArgs) {
    notNullCheck("Trigger args not exist ", triggerArgs, USER);
    List<TriggerArtifactVariable> triggerArtifactVariables = triggerArgs.getTriggerArtifactVariables();

    if (triggerArtifactVariables != null) {
      triggerArtifactVariables.forEach(triggerArtifactVariable -> {
        String entityId = triggerArtifactVariable.getEntityId();
        EntityType entityType = triggerArtifactVariable.getEntityType();
        validateVariableName(appId, entityId, entityType, triggerArtifactVariable.getVariableName());
        if (triggerArtifactVariable.getVariableValue() != null) {
          validateVariableValue(
              appId, triggerArtifactVariable.getVariableValue(), triggerArtifactVariable.getVariableName());
        }
      });
    }
  }

  private void validateVariableValue(String appId, TriggerArtifactSelectionValue variableValue, String variableName) {
    switch (variableValue.getArtifactVariableType()) {
      case ORCHESTRATION:
        TriggerArtifactSelectionWorkflow workflowVar = (TriggerArtifactSelectionWorkflow) variableValue;
        try {
          workflowService.fetchWorkflowName(appId, workflowVar.getWorkflowId());
        } catch (WingsException exception) {
          throw new WingsException("workflow does not exist for variable " + variableName);
        }
        break;
      case PIPELINE:
        TriggerArtifactSelectionPipeline pipelineVar = (TriggerArtifactSelectionPipeline) variableValue;
        try {
          pipelineService.fetchPipelineName(appId, pipelineVar.getPipelineId());
        } catch (WingsException exception) {
          throw new WingsException("pipeline does not exist for variable " + variableName);
        }
        break;
      case ARTIFACT:
        TriggerArtifactSelectionArtifact artifactVar = (TriggerArtifactSelectionArtifact) variableValue;
        ArtifactStream artifactStream = artifactStreamService.get(artifactVar.getArtifactStreamId());
        notNullCheck("artifactStream does not exist for id " + artifactVar.getArtifactStreamId(), artifactStream);
        break;
      default:
        unhandled(variableValue.getArtifactVariableType());
    }
  }

  private void validateVariableName(String appId, String entityId, EntityType entityType, String variableName) {
    switch (entityType) {
      case SERVICE:
      case SERVICE_TEMPLATE:
      case ENVIRONMENT:
        List<ServiceVariable> serviceVariables =
            serviceVariablesService.getServiceVariablesForEntity(appId, entityId, OBTAIN_VALUE);
        boolean variableExists =
            serviceVariables.stream().anyMatch(serviceVariable -> serviceVariable.getName().equals(variableName));
        if (!variableExists) {
          throw new WingsException("variable name " + variableName + " does not exist");
        }

        break;
      case WORKFLOW:
        List<Variable> variables = workflowService.readWorkflow(appId, entityId).getOrchestration().getUserVariables();
        boolean wfVariableExists = variables.stream().anyMatch(variable -> variable.getName().equals(variableName));
        if (!wfVariableExists) {
          throw new WingsException("variable name " + variableName + " does not exist ");
        }
        break;
      default:
        unhandled(entityType);
    }
  }

  private List<DeploymentTrigger> getNewArtifactTriggersForAllApps(String artifactStreamId) {
    return wingsPersistence.createQuery(DeploymentTrigger.class, excludeAuthority)
        .disableValidation()
        .filter(DeploymentTriggerKeys.condition + "."
                + "type",
            NEW_ARTIFACT)
        .filter(DeploymentTriggerKeys.condition + "." + ArtifactTriggerConditionKeys.artifactStreamId, artifactStreamId)
        .asList();
  }
}
