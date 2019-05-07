package software.wings.graphql.datafetcher.instance;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.graphql.datafetcher.instance.instanceInfo.InstanceInfoController;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;
import software.wings.graphql.schema.type.instance.info.QLInstanceType;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class InstanceController {
  private static Map<Class<? extends InstanceInfo>, QLInstanceType> instanceTypeMap = new HashMap<>();
  static {
    Collections.arrayToList(QLInstanceType.values())
        .forEach(type
            -> ((QLInstanceType) type)
                   .getInstanceInfos()
                   .forEach(clazz -> instanceTypeMap.put(clazz, (QLInstanceType) type)));
  }

  @Inject private Map<Class, InstanceInfoController> instanceInfoControllerMap;

  public void populateInstance(@NotNull Instance instance, QLInstanceBuilder builder) {
    builder.id(instance.getUuid())
        .type(instanceTypeMap.get(instance.getInstanceInfo().getClass()))
        .envId(instance.getEnvId())
        .serviceId(instance.getServiceId())
        .lastArtifactId(instance.getLastArtifactId());
    populateInstanceInfo(instance.getInstanceInfo(), builder);
  }

  @VisibleForTesting
  private void populateInstanceInfo(@NotNull InstanceInfo instanceInfo, QLInstanceBuilder qlInstanceBuilder) {
    final InstanceInfoController instanceInfoController = instanceInfoControllerMap.get(instanceInfo.getClass());
    if (instanceInfoController != null) {
      instanceInfoController.populateInstanceInfo(instanceInfo, qlInstanceBuilder);
    } else {
      logger.error("No InstanceInfoMapping found for InstanceInfo : " + instanceInfo.getClass());
    }
  }
}
