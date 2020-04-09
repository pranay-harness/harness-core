package software.wings.sm.states.spotinst;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.spotinst.model.SpotInstConstants.DEFAULT_ELASTIGROUP_MAX_INSTANCES;
import static io.harness.spotinst.model.SpotInstConstants.DEFAULT_ELASTIGROUP_MIN_INSTANCES;
import static io.harness.spotinst.model.SpotInstConstants.DEFAULT_ELASTIGROUP_TARGET_INSTANCES;
import static io.harness.spotinst.model.SpotInstConstants.DEPLOYMENT_ERROR;
import static io.harness.spotinst.model.SpotInstConstants.PHASE_PARAM;
import static io.harness.spotinst.model.SpotInstConstants.SETUP_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.defaultSteadyStateTimeout;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.sm.states.spotinst.SpotInstServiceSetup.SPOTINST_SERVICE_SETUP_COMMAND;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.delegate.task.spotinst.request.SpotinstTrafficShiftAlbSetupParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.delegate.task.spotinst.response.SpotinstTrafficShiftAlbSetupResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.command.SpotinstDummyCommandUnit;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.states.spotinst.SpotinstTrafficShiftAlbSetupElement.SpotinstTrafficShiftAlbSetupElementBuilder;
import software.wings.utils.Misc;
import software.wings.utils.ServiceVersionConvention;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class SpotinstTrafficShiftAlbSetupState extends State {
  @Getter @Setter private String minInstancesExpr;
  @Getter @Setter private String maxInstancesExpr;
  @Getter @Setter private String targetInstancesExpr;
  @Getter @Setter private String elastigroupNamePrefix;
  @Getter @Setter private String timeoutIntervalInMinExpr;
  @Getter @Setter private boolean useCurrentRunningCount;
  @Getter @Setter private List<LbDetailsForAlbTrafficShift> lbDetails;

  @Inject private ActivityService activityService;
  @Inject private DelegateService delegateService;
  @Inject private SpotInstStateHelper spotinstStateHelper;

  @VisibleForTesting
  SpotinstTrafficShiftAlbSetupState() {
    super("stateName", StateType.SPOTINST_ALB_SHIFT_SETUP.name());
  }

  public SpotinstTrafficShiftAlbSetupState(String name) {
    super(name, StateType.SPOTINST_ALB_SHIFT_SETUP.name());
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    SpotInstTaskExecutionResponse executionResponse =
        (SpotInstTaskExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus() == CommandExecutionResult.CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);

    SpotinstTrafficShiftAlbSetupResponse spotinstTrafficShiftAlbSetupResponse =
        (SpotinstTrafficShiftAlbSetupResponse) executionResponse.getSpotInstTaskResponse();

    SpotinstTrafficShiftAlbSetupExecutionData stateExecutionData =
        (SpotinstTrafficShiftAlbSetupExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());
    SpotinstTrafficShiftAlbSetupElementBuilder builder =
        SpotinstTrafficShiftAlbSetupElement.builder()
            .commandName(SPOTINST_SERVICE_SETUP_COMMAND)
            .appId(stateExecutionData.getAppId())
            .envId(stateExecutionData.getEnvId())
            .serviceId(stateExecutionData.getServiceId())
            .infraMappingId(stateExecutionData.getInfraMappingId())
            .elastigroupNamePrefix(Misc.normalizeExpression(context.renderExpression(elastigroupNamePrefix)))
            .timeoutIntervalInMin(
                spotinstStateHelper.renderCount(timeoutIntervalInMinExpr, context, defaultSteadyStateTimeout));

    if (ExecutionStatus.SUCCESS == executionStatus && spotinstTrafficShiftAlbSetupResponse != null) {
      ElastiGroup elastigroupOriginalConfig = stateExecutionData.getElastigroupOriginalConfig();
      ElastiGroup newElastigroupFromDelegate = spotinstTrafficShiftAlbSetupResponse.getNewElastigroup();
      if (newElastigroupFromDelegate != null) {
        elastigroupOriginalConfig.setId(newElastigroupFromDelegate.getId());
        elastigroupOriginalConfig.setName(newElastigroupFromDelegate.getName());
        stateExecutionData.setNewElastigroupId(newElastigroupFromDelegate.getId());
        stateExecutionData.setNewElastigroupName(newElastigroupFromDelegate.getName());
      }
      List<ElastiGroup> groupsToBeDownsized = spotinstTrafficShiftAlbSetupResponse.getElastiGroupsToBeDownsized();
      if (isNotEmpty(groupsToBeDownsized)) {
        ElastiGroup elastigroupToBeDownsized = groupsToBeDownsized.get(0);
        if (useCurrentRunningCount && elastigroupToBeDownsized != null) {
          stateExecutionData.setOldElastigroupId(elastigroupToBeDownsized.getId());
          stateExecutionData.setOldElastigroupName(elastigroupToBeDownsized.getName());
          elastigroupOriginalConfig.getCapacity().setMinimum(elastigroupToBeDownsized.getCapacity().getMinimum());
          elastigroupOriginalConfig.getCapacity().setMaximum(elastigroupToBeDownsized.getCapacity().getMaximum());
          elastigroupOriginalConfig.getCapacity().setTarget(elastigroupToBeDownsized.getCapacity().getTarget());
          builder.oldElastiGroupOriginalConfig(elastigroupToBeDownsized);
        }
      }
      builder.newElastiGroupOriginalConfig(elastigroupOriginalConfig);
      builder.detailsWithTargetGroups(spotinstTrafficShiftAlbSetupResponse.getLbDetailsWithTargetGroups());
    }
    SpotinstTrafficShiftAlbSetupElement contextElement = builder.build();

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .errorMessage(executionResponse.getErrorMessage())
        .stateExecutionData(stateExecutionData)
        .contextElement(contextElement)
        .notifyElement(contextElement)
        .build();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    SpotinstTrafficShiftDataBag dataBag = spotinstStateHelper.getDataBag(context);
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    ServiceElement serviceElement = phaseElement.getServiceElement();

    Artifact artifact = ((DeploymentExecutionContext) context).getDefaultArtifactForService(serviceElement.getUuid());
    notNullCheck("Artifact is null", artifact);

    Activity activity = spotinstStateHelper.createActivity(context, artifact, getStateType(),
        SPOTINST_SERVICE_SETUP_COMMAND, CommandUnitDetails.CommandUnitType.SPOTINST_SETUP,
        ImmutableList.of(
            new SpotinstDummyCommandUnit(SETUP_COMMAND_UNIT), new SpotinstDummyCommandUnit(DEPLOYMENT_ERROR)));

    String finalElastigroupNamePrefix = isBlank(elastigroupNamePrefix)
        ? ServiceVersionConvention.getPrefix(
              dataBag.getApp().getName(), serviceElement.getName(), dataBag.getEnv().getName())
        : context.renderExpression(elastigroupNamePrefix);
    finalElastigroupNamePrefix = Misc.normalizeExpression(finalElastigroupNamePrefix);

    String elastigroupOriginalJson =
        context.renderExpression(dataBag.getInfrastructureMapping().getSpotinstElastiGroupJson());
    ElastiGroup elastigroupOriginalConfig = spotinstStateHelper.generateConfigFromJson(elastigroupOriginalJson);
    ElastiGroupCapacity groupCapacity = elastigroupOriginalConfig.getCapacity();
    if (useCurrentRunningCount) {
      groupCapacity.setMinimum(DEFAULT_ELASTIGROUP_MIN_INSTANCES);
      groupCapacity.setMaximum(DEFAULT_ELASTIGROUP_MAX_INSTANCES);
      groupCapacity.setTarget(DEFAULT_ELASTIGROUP_TARGET_INSTANCES);
    } else {
      groupCapacity.setMinimum(
          spotinstStateHelper.renderCount(minInstancesExpr, context, DEFAULT_ELASTIGROUP_MIN_INSTANCES));
      groupCapacity.setMaximum(
          spotinstStateHelper.renderCount(maxInstancesExpr, context, DEFAULT_ELASTIGROUP_MAX_INSTANCES));
      groupCapacity.setTarget(
          spotinstStateHelper.renderCount(targetInstancesExpr, context, DEFAULT_ELASTIGROUP_TARGET_INSTANCES));
    }

    SpotinstTrafficShiftAlbSetupParameters parameters =
        SpotinstTrafficShiftAlbSetupParameters.builder()
            .appId(dataBag.getApp().getUuid())
            .accountId(dataBag.getApp().getAccountId())
            .activityId(activity.getUuid())
            .commandName(SPOTINST_SERVICE_SETUP_COMMAND)
            .workflowExecutionId(context.getWorkflowExecutionId())
            .timeoutIntervalInMin(
                spotinstStateHelper.renderCount(timeoutIntervalInMinExpr, context, defaultSteadyStateTimeout))
            .awsRegion(dataBag.getInfrastructureMapping().getRegion())
            .elastigroupJson(elastigroupOriginalJson)
            .elastigroupNamePrefix(finalElastigroupNamePrefix)
            .image(artifact.getRevision())
            .lbDetails(getRenderedLbDetails(context))
            .userData(spotinstStateHelper.getBase64EncodedUserData(
                dataBag.getApp().getUuid(), serviceElement.getUuid(), context))
            .build();

    SpotInstCommandRequest commandRequest = SpotInstCommandRequest.builder()
                                                .spotInstTaskParameters(parameters)
                                                .awsConfig(dataBag.getAwsConfig())
                                                .spotInstConfig(dataBag.getSpotinstConfig())
                                                .awsEncryptionDetails(dataBag.getAwsEncryptedDataDetails())
                                                .spotinstEncryptionDetails(dataBag.getSpotinstEncryptedDataDetails())
                                                .build();

    SpotinstTrafficShiftAlbSetupExecutionData stateExecutionData =
        SpotinstTrafficShiftAlbSetupExecutionData.builder()
            .activityId(activity.getUuid())
            .serviceId(serviceElement.getUuid())
            .envId(dataBag.getEnv().getUuid())
            .appId(dataBag.getApp().getUuid())
            .infraMappingId(dataBag.getInfrastructureMapping().getUuid())
            .commandName(SPOTINST_SERVICE_SETUP_COMMAND)
            .elastigroupOriginalConfig(elastigroupOriginalConfig)
            .build();

    DelegateTask delegateTask = spotinstStateHelper.getDelegateTask(dataBag.getApp().getAccountId(),
        dataBag.getApp().getUuid(), TaskType.SPOTINST_COMMAND_TASK, activity.getUuid(), dataBag.getEnv().getUuid(),
        dataBag.getInfrastructureMapping().getUuid(), commandRequest);

    delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .correlationIds(singletonList(activity.getUuid()))
        .stateExecutionData(stateExecutionData)
        .async(true)
        .build();
  }

  private List<LbDetailsForAlbTrafficShift> getRenderedLbDetails(ExecutionContext context) {
    List<LbDetailsForAlbTrafficShift> rendered = new ArrayList<>();
    for (LbDetailsForAlbTrafficShift originalLbDetails : lbDetails) {
      rendered.add(LbDetailsForAlbTrafficShift.builder()
                       .loadBalancerName(context.renderExpression(originalLbDetails.getLoadBalancerName()))
                       .loadBalancerArn(context.renderExpression(originalLbDetails.getListenerPort()))
                       .listenerPort(context.renderExpression(originalLbDetails.getListenerPort()))
                       .listenerArn(context.renderExpression(originalLbDetails.getListenerArn()))
                       .useSpecificRule(originalLbDetails.isUseSpecificRule())
                       .ruleArn(context.renderExpression(originalLbDetails.getRuleArn()))
                       .build());
    }
    return rendered;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // Do nothing on abort
  }
}