package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.service.impl.aws.model.AwsConstants.AWS_AMI_ALL_PHASE_ROLLBACK_NAME;
import static software.wings.service.impl.aws.model.AwsConstants.PHASE_PARAM;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Lists;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutput.Scope;
import io.harness.context.ContextElementType;
import io.harness.serializer.KryoUtils;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.AmiServiceDeployElement;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.AwsAmiDeployStateExecutionData;
import software.wings.api.ContainerServiceData;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsAmiAllPhaseRollbackData;
import software.wings.service.impl.aws.model.AwsAmiPreDeploymentData;
import software.wings.service.impl.aws.model.AwsAmiResizeData;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployResponse;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

public class AwsAmiServiceRollback extends AwsAmiServiceDeployState {
  @Getter @Setter @Attributes(title = "Rollback all phases at once") private boolean rollbackAllPhasesAtOnce;

  public AwsAmiServiceRollback(String name) {
    super(name, StateType.AWS_AMI_SERVICE_ROLLBACK.name());
  }

  @Override
  protected ExecutionResponse executeInternal(ExecutionContext context) {
    if (allPhaseRollbackDone(context)) {
      return anExecutionResponse().withExecutionStatus(ExecutionStatus.SUCCESS).build();
    }

    AmiServiceSetupElement serviceSetupElement = context.getContextElement(ContextElementType.AMI_SERVICE_SETUP);
    AmiServiceDeployElement amiServiceDeployElement = context.getContextElement(ContextElementType.AMI_SERVICE_DEPLOY);
    List<AwsAmiResizeData> oldAsgCounts = Lists.newArrayList();
    String newAgName;
    int newAsgFinalDesiredCount;
    if (rollbackAllPhasesAtOnce) {
      AwsAmiPreDeploymentData preDeploymentData = serviceSetupElement.getPreDeploymentData();
      Map<String, Integer> asgNameToDesiredCapacity = preDeploymentData.getAsgNameToDesiredCapacity();
      if (isNotEmpty(asgNameToDesiredCapacity)) {
        asgNameToDesiredCapacity.forEach(
            (key, value) -> oldAsgCounts.add(AwsAmiResizeData.builder().asgName(key).desiredCount(value).build()));
      }
      newAgName = serviceSetupElement.getNewAutoScalingGroupName();
      newAsgFinalDesiredCount = 0;
    } else {
      List<ContainerServiceData> oldData = amiServiceDeployElement.getOldInstanceData();
      if (isNotEmpty(oldData)) {
        oldData.forEach(data -> {
          oldAsgCounts.add(
              AwsAmiResizeData.builder().asgName(data.getName()).desiredCount(data.getPreviousCount()).build());
        });
      }
      ContainerServiceData newContainerServiceData = amiServiceDeployElement.getNewInstanceData().get(0);
      newAgName = newContainerServiceData.getName();
      newAsgFinalDesiredCount = newContainerServiceData.getPreviousCount();
    }

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    Activity activity = crateActivity(context);

    AwsAmiInfrastructureMapping infrastructureMapping = (AwsAmiInfrastructureMapping) infrastructureMappingService.get(
        activity.getAppId(), phaseElement.getInfraMappingId());
    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

    String region = infrastructureMapping.getRegion();
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId());

    // If resize new first is selected as true by the user in setup state. Then in rollback we want to resize the
    // list of older Asgs first. In that case, the flag sent to the delegate would need to be reversed.
    boolean resizeNewFirst = !RESIZE_NEW_FIRST.equals(serviceSetupElement.getResizeStrategy());

    createAndQueueResizeTask(awsConfig, encryptionDetails, region, infrastructureMapping.getAccountId(),
        infrastructureMapping.getAppId(), activity.getUuid(), getCommandName(), resizeNewFirst, newAgName,
        newAsgFinalDesiredCount, oldAsgCounts, serviceSetupElement.getAutoScalingSteadyStateTimeout(),
        infrastructureMapping.getEnvId(), serviceSetupElement.getMinInstances(), serviceSetupElement.getMaxInstances(),
        serviceSetupElement.getPreDeploymentData(), infrastructureMapping.getClassicLoadBalancers(),
        infrastructureMapping.getTargetGroupArns(), true, serviceSetupElement.getBaseScalingPolicyJSONs(),
        serviceSetupElement.getDesiredInstances());

    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData = prepareStateExecutionData(activity.getUuid(),
        serviceSetupElement, amiServiceDeployElement.getInstanceCount(), amiServiceDeployElement.getInstanceUnitType(),
        amiServiceDeployElement.getNewInstanceData(), amiServiceDeployElement.getOldInstanceData());
    awsAmiDeployStateExecutionData.setRollback(true);

    return anExecutionResponse()
        .withAsync(true)
        .withStateExecutionData(awsAmiDeployStateExecutionData)
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .addCorrelationIds(activity.getUuid())
        .build();
  }

  @Override
  protected List<InstanceElement> handleAsyncInternal(AwsAmiServiceDeployResponse amiServiceDeployResponse,
      ExecutionContext context, AmiServiceSetupElement serviceSetupElement,
      ManagerExecutionLogCallback executionLogCallback) {
    if (rollbackAllPhasesAtOnce && ExecutionStatus.SUCCESS.equals(amiServiceDeployResponse.getExecutionStatus())) {
      markAllPhaseRollbackDone(context);
    }
    return emptyList();
  }

  private void markAllPhaseRollbackDone(ExecutionContext context) {
    sweepingOutputService.save(
        context.prepareSweepingOutputBuilder(Scope.WORKFLOW)
            .name(AWS_AMI_ALL_PHASE_ROLLBACK_NAME)
            .output(KryoUtils.asDeflatedBytes(AwsAmiAllPhaseRollbackData.builder().allPhaseRollbackDone(true).build()))
            .build());
  }

  private boolean allPhaseRollbackDone(ExecutionContext context) {
    SweepingOutput sweepingOutputInput =
        context.prepareSweepingOutputBuilder(Scope.WORKFLOW).name(AWS_AMI_ALL_PHASE_ROLLBACK_NAME).build();
    SweepingOutput result = sweepingOutputService.find(sweepingOutputInput.getAppId(), sweepingOutputInput.getName(),
        sweepingOutputInput.getPipelineExecutionId(), sweepingOutputInput.getWorkflowExecutionId(),
        sweepingOutputInput.getPhaseExecutionId(), sweepingOutputInput.getStateExecutionId());
    if (result == null) {
      return false;
    }
    return ((AwsAmiAllPhaseRollbackData) KryoUtils.asInflatedObject(result.getOutput())).isAllPhaseRollbackDone();
  }

  @Override
  @SchemaIgnore
  public String getCommandName() {
    return super.getCommandName();
  }

  @Override
  @SchemaIgnore
  public int getInstanceCount() {
    return super.getInstanceCount();
  }

  @Override
  @SchemaIgnore
  public InstanceUnitType getInstanceUnitType() {
    return super.getInstanceUnitType();
  }
}