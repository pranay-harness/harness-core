package software.wings.sm.states;

import lombok.extern.slf4j.Slf4j;
import software.wings.api.ContainerServiceElement;
import software.wings.helpers.ext.ecs.request.EcsListenerUpdateRequestConfigData;
import software.wings.sm.StateType;

@Slf4j
public class EcsBGUpdateListnerRollbackState extends EcsBGUpdateListnerState {
  public EcsBGUpdateListnerRollbackState(String name) {
    super(name, StateType.ECS_LISTENER_UPDATE_ROLLBACK.name());
  }

  @Override
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
