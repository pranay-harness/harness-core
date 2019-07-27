package software.wings.service.impl.trigger;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static net.redhogs.cronparser.CronExpressionDescriptor.getDescription;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import net.redhogs.cronparser.DescriptionTypeEnum;
import net.redhogs.cronparser.I18nMessages;
import net.redhogs.cronparser.Options;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.trigger.Action;
import software.wings.beans.trigger.Action.ActionType;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.ScheduledTriggerJob;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;

import java.util.List;

@Singleton
@Slf4j
public class DeploymentTriggerServiceHelper {
  @Inject private transient WingsPersistence wingsPersistence;
  @Inject private transient TriggerArtifactVariableHandler artifactVariableHandler;
  @Inject private transient PipelineService pipelineService;
  @Inject private transient WorkflowService workflowService;

  public List<DeploymentTrigger> getTriggersByApp(String appId) {
    return wingsPersistence.query(DeploymentTrigger.class, aPageRequest().addFilter("appId", EQ, appId).build())
        .getResponse();
  }

  public void validateTriggerAction(DeploymentTrigger trigger) {
    Action action = trigger.getAction();
    if (action.getActionType() == ActionType.PIPELINE) {
      PipelineAction pipelineAction = (PipelineAction) action;
      try {
        pipelineService.fetchPipelineName(trigger.getAppId(), pipelineAction.getPipelineId());
      } catch (WingsException exception) {
        throw new WingsException("Pipeline does not exist for pipeline id " + pipelineAction.getPipelineId());
      }

      validateTriggerArgs(trigger.getAppId(), pipelineAction.getTriggerArgs());
    } else if (action.getActionType() == ActionType.ORCHESTRATION) {
      WorkflowAction workflowAction = (WorkflowAction) action;
      try {
        workflowService.fetchWorkflowName(trigger.getAppId(), workflowAction.getWorkflowId());
      } catch (WingsException exception) {
        throw new WingsException("workflow does not exist for workflowId " + workflowAction.getWorkflowId());
      }

      validateTriggerArgs(trigger.getAppId(), workflowAction.getTriggerArgs());
    }
  }

  public void reBuildTriggerActionWithNames(DeploymentTrigger deploymentTrigger) {
    switch (deploymentTrigger.getAction().getActionType()) {
      case PIPELINE:
        PipelineAction pipelineAction = (PipelineAction) deploymentTrigger.getAction();
        TriggerArgs triggerArgs = pipelineAction.getTriggerArgs();
        List<TriggerArtifactVariable> triggerArtifactVariables =
            artifactVariableHandler.transformTriggerArtifactVariables(
                deploymentTrigger.getAppId(), triggerArgs.getTriggerArtifactVariables());

        deploymentTrigger.setAction(
            PipelineAction.builder()
                .pipelineId(pipelineAction.getPipelineId())
                .pipelineName(
                    pipelineService.fetchPipelineName(deploymentTrigger.getAppId(), pipelineAction.getPipelineId()))
                .triggerArgs(TriggerArgs.builder()
                                 .excludeHostsWithSameArtifact(triggerArgs.isExcludeHostsWithSameArtifact())
                                 .variables(triggerArgs.getVariables())
                                 .triggerArtifactVariables(triggerArtifactVariables)
                                 .build())
                .build());
        break;
      case ORCHESTRATION:
        WorkflowAction workflowAction = (WorkflowAction) deploymentTrigger.getAction();
        TriggerArgs wfTriggerArgs = workflowAction.getTriggerArgs();
        List<TriggerArtifactVariable> wfTriggerArtifactVariables =
            artifactVariableHandler.transformTriggerArtifactVariables(
                deploymentTrigger.getAppId(), wfTriggerArgs.getTriggerArtifactVariables());
        deploymentTrigger.setAction(
            WorkflowAction.builder()
                .workflowId(workflowAction.getWorkflowId())
                .workflowName(
                    workflowService.fetchWorkflowName(deploymentTrigger.getAppId(), workflowAction.getWorkflowId()))
                .triggerArgs(TriggerArgs.builder()
                                 .excludeHostsWithSameArtifact(wfTriggerArgs.isExcludeHostsWithSameArtifact())
                                 .variables(wfTriggerArgs.getVariables())
                                 .triggerArtifactVariables(wfTriggerArtifactVariables)
                                 .build())
                .build());
        break;
      default:
        unhandled(deploymentTrigger.getAction().getActionType());
    }
  }

  public static String getCronDescription(String cronExpression) {
    try {
      String description = getDescription(DescriptionTypeEnum.FULL, ScheduledTriggerJob.PREFIX + cronExpression,
          new Options(), I18nMessages.DEFAULT_LOCALE);
      return StringUtils.lowerCase("" + description.charAt(0)) + description.substring(1);
    } catch (Exception e) {
      throw new WingsException(INVALID_ARGUMENT, USER).addParam("args", "Invalid cron expression" + cronExpression);
    }
  }

  private void validateTriggerArgs(String appId, TriggerArgs triggerArgs) {
    notNullCheck("Trigger args not exist ", triggerArgs, USER);
    List<TriggerArtifactVariable> triggerArtifactVariables = triggerArgs.getTriggerArtifactVariables();

    if (triggerArtifactVariables != null) {
      artifactVariableHandler.validateTriggerArtifactVariables(appId, triggerArtifactVariables);
    }
  }
}
