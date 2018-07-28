package software.wings.sm.states;

import software.wings.api.AmiServiceDeployElement;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.AwsAmiDeployStateExecutionData;
import software.wings.api.ContainerServiceData;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.SettingAttribute;
import software.wings.common.Constants;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployResponse;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

import java.util.Collections;
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

    ContainerServiceData oldContainerServiceData = amiServiceDeployElement.getOldInstanceData().get(0);
    ContainerServiceData newContainerServiceData = amiServiceDeployElement.getNewInstanceData().get(0);

    AwsAmiInfrastructureMapping infrastructureMapping = (AwsAmiInfrastructureMapping) infrastructureMappingService.get(
        activity.getAppId(), phaseElement.getInfraMappingId());
    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

    String region = infrastructureMapping.getRegion();
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId());
    boolean resizeNewFirst = serviceSetupElement.getResizeStrategy().equals(ResizeStrategy.RESIZE_NEW_FIRST);

    createAndQueueResizeTask(awsConfig, encryptionDetails, region, infrastructureMapping.getAccountId(),
        infrastructureMapping.getAppId(), activity.getUuid(), getCommandName(), resizeNewFirst,
        oldContainerServiceData.getName(), oldContainerServiceData.getPreviousCount(),
        newContainerServiceData.getName(), newContainerServiceData.getPreviousCount(),
        serviceSetupElement.getAutoScalingSteadyStateTimeout(), infrastructureMapping.getEnvId());

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
    return Collections.emptyList();
  }
}
