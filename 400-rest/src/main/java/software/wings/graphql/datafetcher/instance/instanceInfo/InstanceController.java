package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.graphql.schema.type.instance.QLInstance;

public interface InstanceController<T extends QLInstance> {
  T populateInstance(Instance instance);
}
