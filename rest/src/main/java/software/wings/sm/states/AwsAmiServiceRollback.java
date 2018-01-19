package software.wings.sm.states;

import software.wings.api.AmiServiceDeployElement;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.AwsAmiDeployStateExecutionData;
import software.wings.api.ContainerServiceData;
import software.wings.api.InstanceElement;
import software.wings.beans.Activity;
import software.wings.beans.AwsConfig;
import software.wings.beans.ResizeStrategy;
import software.wings.security.encryption.EncryptedDataDetail;
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
    AmiServiceSetupElement serviceSetupElement = context.getContextElement(ContextElementType.AMI_SERVICE_SETUP);
    AmiServiceDeployElement amiServiceDeployElement = context.getContextElement(ContextElementType.AMI_SERVICE_DEPLOY);
    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData = prepareStateExecutionData(activity.getUuid(),
        serviceSetupElement, amiServiceDeployElement.getInstanceCount(), amiServiceDeployElement.getInstanceUnitType(),
        amiServiceDeployElement.getNewInstanceData(), amiServiceDeployElement.getOldInstanceData());
    awsAmiDeployStateExecutionData.setRollback(true);
    return awsAmiDeployStateExecutionData;
  }

  @Override
  protected List<InstanceElement> handleAsyncInternal(ExecutionContext context, String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, AmiServiceSetupElement serviceSetupElement,
      ManagerExecutionLogCallback executionLogCallback) {
    AmiServiceDeployElement amiServiceDeployElement = context.getContextElement(ContextElementType.AMI_SERVICE_DEPLOY);
    // TODO: old and new both should be present with 1 element atleast
    ContainerServiceData oldContainerServiceData = amiServiceDeployElement.getOldInstanceData().get(0);
    ContainerServiceData newContainerServiceData = amiServiceDeployElement.getNewInstanceData().get(0);

    resizeAsgs(region, awsConfig, encryptionDetails, oldContainerServiceData.getName(),
        oldContainerServiceData.getPreviousCount(), newContainerServiceData.getName(),
        newContainerServiceData.getPreviousCount(), executionLogCallback,
        serviceSetupElement.getResizeStrategy() == ResizeStrategy.RESIZE_NEW_FIRST,
        serviceSetupElement.getAutoScalingSteadyStateTimeout());
    return Collections.emptyList();
  }
}
