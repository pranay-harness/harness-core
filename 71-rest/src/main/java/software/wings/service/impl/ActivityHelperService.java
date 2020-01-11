package software.wings.service.impl;

import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.data.structure.EmptyPredicate;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.api.InstanceElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnit;
import software.wings.service.intfc.ActivityService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 2019-02-26.
 */
@Singleton
@ValidateOnExecution
public class ActivityHelperService {
  @Inject private ActivityService activityService;

  public Activity createAndSaveActivity(@NotNull ExecutionContext executionContext, @NotNull Type activityType,
      @NotEmpty String commandName, @NotEmpty String commandUnitType, @NotNull List<CommandUnit> commandUnits) {
    return createAndSaveActivity(executionContext, activityType, commandName, commandUnitType, commandUnits, null);
  }

  public Activity createAndSaveActivity(ExecutionContext executionContext, Type activityType, String commandName,
      String commandUnitType, List<CommandUnit> commandUnits, Artifact artifact) {
    ActivityBuilder activityBuilder =
        getActivityBuilder(executionContext, activityType, commandName, commandUnitType, commandUnits, artifact);
    return activityService.save(activityBuilder.build());
  }

  public ActivityBuilder getActivityBuilder(ExecutionContext executionContext, Type activityType, String commandName,
      String commandUnitType, List<CommandUnit> commandUnits, Artifact artifact) {
    Application app = ((ExecutionContextImpl) executionContext).fetchRequiredApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);
    WorkflowStandardParams workflowStandardParams =
        Objects.requireNonNull(executionContext.getContextElement(ContextElementType.STANDARD));

    ActivityBuilder activityBuilder =
        Activity.builder()
            .appId(app.getUuid())
            .applicationName(app.getName())
            .type(activityType)
            .workflowExecutionId(executionContext.getWorkflowExecutionId())
            .workflowType(executionContext.getWorkflowType())
            .workflowId(executionContext.getWorkflowId())
            .workflowExecutionName(executionContext.getWorkflowExecutionName())
            .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
            .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
            .status(ExecutionStatus.RUNNING)
            .commandName(commandName)
            .commandType(commandUnitType)
            .commandUnits(EmptyPredicate.isEmpty(commandUnits) ? new ArrayList<>() : commandUnits)
            .triggeredBy(TriggeredBy.builder()
                             .email(workflowStandardParams.getCurrentUser().getEmail())
                             .name(workflowStandardParams.getCurrentUser().getName())
                             .build());

    if (artifact != null) {
      activityBuilder.artifactStreamId(artifact.getArtifactStreamId())
          .artifactStreamName(artifact.getArtifactSourceName())
          .artifactName(artifact.getDisplayName())
          .artifactId(artifact.getUuid());
    }

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType() == BUILD) {
      activityBuilder.environmentId(GLOBAL_ENV_ID).environmentName(GLOBAL_ENV_ID).environmentType(ALL);
    } else {
      activityBuilder.environmentId(env.getUuid())
          .environmentName(env.getName())
          .environmentType(env.getEnvironmentType());
    }

    if (instanceElement != null && instanceElement.getServiceTemplateElement() != null) {
      activityBuilder.serviceTemplateId(instanceElement.getServiceTemplateElement().getUuid())
          .serviceTemplateName(instanceElement.getServiceTemplateElement().getName())
          .serviceId(instanceElement.getServiceTemplateElement().getServiceElement().getUuid())
          .serviceName(instanceElement.getServiceTemplateElement().getServiceElement().getName())
          .serviceInstanceId(instanceElement.getUuid())
          .hostName(instanceElement.getHost().getHostName())
          .publicDns(instanceElement.getHost().getPublicDns());
    }
    return activityBuilder;
  }

  public void updateStatus(@NotEmpty String activityId, @NotEmpty String appId, @NotNull ExecutionStatus status) {
    activityService.updateStatus(activityId, appId, status);
  }
}
