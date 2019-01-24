package software.wings.sm.states;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ContainerServiceElement;
import software.wings.helpers.ext.ecs.request.EcsListenerUpdateRequestConfigData;
import software.wings.sm.StateType;

public class EcsBGUpdateListnerRollbackState extends EcsBGUpdateListnerState {
  private static final Logger logger = LoggerFactory.getLogger(EcsBGUpdateListnerRollbackState.class);

  public EcsBGUpdateListnerRollbackState(String name) {
    super(name, StateType.ECS_LISTENER_UPDATE_ROLLBACK.name());
  }

  protected EcsListenerUpdateRequestConfigData getEcsListenerUpdateRequestConfigData(
      ContainerServiceElement containerServiceElement) {
    EcsListenerUpdateRequestConfigData configData =
        super.getEcsListenerUpdateRequestConfigData(containerServiceElement);
    configData.setRollback(true);
    configData.setServiceNameDownsized(containerServiceElement.getEcsBGSetupData().getDownsizedServiceName());
    configData.setServiceCountDownsized(containerServiceElement.getEcsBGSetupData().getDownsizedServiceCount());
    configData.setTargetGroupForNewService(containerServiceElement.getTargetGroupForNewService());
    return configData;
  }
}
