package software.wings.graphql.datafetcher.instance;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;

import javax.validation.constraints.NotNull;

public class InstanceController {
  public static void populateInstance(@NotNull Instance instance, QLInstanceBuilder builder) {
    builder.id(instance.getUuid()).type(instance.getInstanceType());
  }
}
