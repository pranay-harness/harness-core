package software.wings.graphql.schema.type.trigger;

import io.harness.beans.WorkflowType;
import lombok.experimental.UtilityClass;
import software.wings.beans.trigger.Trigger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class TriggerActionController {
  public static QLTriggerAction populateTriggerAction(Trigger trigger) {
    QLTriggerAction triggerAction = null;

    List<QLArtifactSelection> artifactSelections =
        trigger.getArtifactSelections()
            .stream()
            .map(e -> {
              QLArtifactSelection artifactSelection = null;
              switch (e.getType()) {
                case ARTIFACT_SOURCE:
                  artifactSelection = QLFromTriggeringArtifactSource.builder()
                                          .serviceId(e.getServiceId())
                                          .serviceName(e.getServiceName())
                                          .build();
                  break;
                case LAST_COLLECTED:
                  artifactSelection = QLLastCollected.builder()
                                          .serviceId(e.getServiceId())
                                          .serviceName(e.getServiceName())
                                          .artifactSourceId(e.getArtifactStreamId())
                                          .artifactSourceName(e.getArtifactSourceName())
                                          .artifactFilter(e.getArtifactFilter())
                                          .regex(e.isRegex())
                                          .build();
                  break;
                case LAST_DEPLOYED:
                  if (trigger.getWorkflowType() == WorkflowType.PIPELINE) {
                    artifactSelection = QLLastDeployedFromPipeline.builder()
                                            .serviceId(e.getServiceId())
                                            .serviceName(e.getServiceName())
                                            .pipelineId(e.getPipelineId())
                                            .pipelineName(e.getPipelineName())
                                            .build();
                  } else {
                    artifactSelection = QLLastDeployedFromWorkflow.builder()
                                            .serviceId(e.getServiceId())
                                            .serviceName(e.getServiceName())
                                            .workflowId(e.getWorkflowId())
                                            .workflowName(e.getWorkflowName())
                                            .build();
                  }
                  break;
                case PIPELINE_SOURCE:
                  artifactSelection = QLFromTriggeringPipeline.builder()
                                          .serviceId(e.getServiceId())
                                          .serviceName(e.getServiceName())
                                          .build();
                  break;
                case WEBHOOK_VARIABLE:
                  artifactSelection = QLFromWebhookPayload.builder()
                                          .serviceId(e.getServiceId())
                                          .serviceName(e.getServiceName())
                                          .artifactSourceId(e.getArtifactStreamId())
                                          .artifactSourceName(e.getArtifactSourceName())
                                          .build();
                  break;
                default:
              }
              return artifactSelection;
            })
            .collect(Collectors.toList());

    List<QLTriggerVariableValue> variableValues = new ArrayList<>();
    if (trigger.getWorkflowVariables() != null) {
      trigger.getWorkflowVariables().forEach(
          (key, value) -> variableValues.add(QLTriggerVariableValue.builder().name(key).value(value).build()));
    }

    if (trigger.getWorkflowType() == WorkflowType.PIPELINE) {
      triggerAction = QLPipelineAction.builder()
                          .pipelineId(trigger.getWorkflowId())
                          .pipelineName(trigger.getWorkflowName())
                          .artifactSelections(artifactSelections)
                          .variables(variableValues)
                          .build();
    } else {
      triggerAction = QLWorkflowAction.builder()
                          .workflowId(trigger.getWorkflowId())
                          .workflowName(trigger.getWorkflowName())
                          .artifactSelections(artifactSelections)
                          .variables(variableValues)
                          .build();
    }
    return triggerAction;
  }
}
