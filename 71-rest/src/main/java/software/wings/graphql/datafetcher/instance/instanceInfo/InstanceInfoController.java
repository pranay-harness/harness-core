package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;

public interface InstanceInfoController<T extends InstanceInfo> {
  void populateInstanceInfo(T instanceInfo, QLInstanceBuilder builder);
}
