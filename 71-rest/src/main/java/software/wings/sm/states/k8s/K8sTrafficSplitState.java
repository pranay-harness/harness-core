package software.wings.sm.states.k8s;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.ExceptionUtils.getMessage;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.K8S_TRAFFIC_SPLIT;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.beans.k8s.istio.IstioDestinationWeight;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType;
import software.wings.helpers.ext.k8s.request.K8sTrafficSplitTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class K8sTrafficSplitState extends State {
  @Inject private K8sStateHelper k8sStateHelper;
  @Inject private ActivityService activityService;

  public static final String K8S_TRAFFIC_SPLIT_STATE_NAME = "Traffic Split";

  public K8sTrafficSplitState(String name) {
    super(name, K8S_TRAFFIC_SPLIT.name());
  }

  public static final String K8S_VIRTUAL_SERVICE = "VIRTUAL_SERVICE";
  public static final String K8S_VIRTUAL_SERVICE_PLACEDOLDER = "${VIRTUAL_SERVICE}";
  public static final String K8S_CANARY_DESTINATION = "k8s.canaryDestination";
  public static final String K8S_CANARY_DESTINATION_PLACEHOLDER = "${k8s.canaryDestination}";
  public static final String K8S_STABLE_DESTINATION = "k8s.stableDestination";
  public static final String K8S_STABLE_DESTINATION_PLACEHOLDER = "${k8s.stableDestination}";

  @Trimmed @Getter @Setter @Attributes(title = "Virtual Service Name") private String virtualServiceName;
  @Getter
  @Setter
  @Attributes(title = "Destination Weights")
  private List<IstioDestinationWeight> istioDestinationWeights;

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      logger.info("Executing K8sTrafficSplitState");
      sanitizeStateParameters();

      ContainerInfrastructureMapping infraMapping = k8sStateHelper.getContainerInfrastructureMapping(context);
      Activity activity = createActivity(context);

      renderIstioDestinationWeights(context);

      K8sTaskParameters k8sTaskParameters = K8sTrafficSplitTaskParameters.builder()
                                                .activityId(activity.getUuid())
                                                .commandName(K8S_TRAFFIC_SPLIT_STATE_NAME)
                                                .releaseName(k8sStateHelper.getReleaseName(context, infraMapping))
                                                .k8sTaskType(K8sTaskType.TRAFFIC_SPLIT)
                                                .timeoutIntervalInMin(10)
                                                .virtualServiceName(virtualServiceName)
                                                .istioDestinationWeights(istioDestinationWeights)
                                                .build();
      return k8sStateHelper.queueK8sDelegateTask(context, k8sTaskParameters);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      String appId = workflowStandardParams.getAppId();

      K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();
      ExecutionStatus executionStatus =
          executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                               : ExecutionStatus.FAILED;

      activityService.updateStatus(k8sStateHelper.getActivityId(context), appId, executionStatus);
      K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
      stateExecutionData.setStatus(executionStatus);

      return anExecutionResponse()
          .withStateExecutionData(context.getStateExecutionData())
          .withExecutionStatus(executionStatus)
          .build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  private Activity createActivity(ExecutionContext context) {
    return k8sStateHelper.createK8sActivity(context, K8S_TRAFFIC_SPLIT_STATE_NAME, getStateType(), activityService,
        ImmutableList.of(new K8sDummyCommandUnit(K8sDummyCommandUnit.Init),
            new K8sDummyCommandUnit(K8sDummyCommandUnit.TrafficSplit)));
  }

  private void renderIstioDestinationWeights(ExecutionContext context) {
    if (isNotEmpty(istioDestinationWeights)) {
      for (IstioDestinationWeight ruleWithWeight : istioDestinationWeights) {
        if (isNotBlank(ruleWithWeight.getDestination())) {
          ruleWithWeight.setDestination(context.renderExpression(ruleWithWeight.getDestination()));
        }
      }
    }
  }

  private void sanitizeStateParameters() {
    if (isNotBlank(virtualServiceName)) {
      virtualServiceName = virtualServiceName.trim();
    }

    for (IstioDestinationWeight istioDestinationWeight : istioDestinationWeights) {
      if (isNotBlank(istioDestinationWeight.getDestination())) {
        istioDestinationWeight.setDestination(istioDestinationWeight.getDestination().trim());
      }
    }
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (isBlank(virtualServiceName)) {
      invalidFields.put("VirtualService Name", "VirtualService must not be blank");
    }

    return invalidFields;
  }
}
