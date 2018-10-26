package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;

import com.google.common.collect.Lists;

import software.wings.api.AmiServiceDeployElement;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.AwsAmiDeployStateExecutionData;
import software.wings.api.ContainerServiceData;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.common.Constants;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsAmiResizeData;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployResponse;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

import java.util.List;

/**
 * Created by anubhaw on 12/19/17.
 */
public class AwsAmiServiceRollback extends AwsAmiServiceDeployState {
  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public AwsAmiServiceRollback(String name) {
    super(name, StateType.AWS_AMI_SERVICE_ROLLBACK.name());
  }

  @Override
  protected AwsAmiDeployStateExecutionData prepareStateExecutionData(ExecutionContext context, Activity activity) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    AmiServiceSetupElement serviceSetupElement = context.getContextElement(ContextElementType.AMI_SERVICE_SETUP);
    AmiServiceDeployElement amiServiceDeployElement = context.getContextElement(ContextElementType.AMI_SERVICE_DEPLOY);

    List<ContainerServiceData> oldData = amiServiceDeployElement.getOldInstanceData();
    List<AwsAmiResizeData> oldAsgCounts = Lists.newArrayList();
    if (isNotEmpty(oldData)) {
      oldData.forEach(data -> {
        oldAsgCounts.add(
            AwsAmiResizeData.builder().asgName(data.getName()).desiredCount(data.getPreviousCount()).build());
      });
    }

    ContainerServiceData newContainerServiceData = amiServiceDeployElement.getNewInstanceData().get(0);
    String newAgName = newContainerServiceData.getName();
    int newAsgFinalDesiredCount = newContainerServiceData.getPreviousCount();

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
        infrastructureMapping.getTargetGroupArns(), true);

    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData = prepareStateExecutionData(activity.getUuid(),
        serviceSetupElement, amiServiceDeployElement.getInstanceCount(), amiServiceDeployElement.getInstanceUnitType(),
        amiServiceDeployElement.getNewInstanceData(), amiServiceDeployElement.getOldInstanceData());
    awsAmiDeployStateExecutionData.setRollback(true);
    return awsAmiDeployStateExecutionData;
  }

  @Override
  protected List<InstanceElement> handleAsyncInternal(AwsAmiServiceDeployResponse amiServiceDeployResponse,
      ExecutionContext context, AmiServiceSetupElement serviceSetupElement,
      ManagerExecutionLogCallback executionLogCallback) {
    return emptyList();
  }
}
