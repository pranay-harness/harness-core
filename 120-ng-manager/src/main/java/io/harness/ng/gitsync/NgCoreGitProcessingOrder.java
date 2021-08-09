package io.harness.ng.gitsync;

import io.harness.EntityType;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NgCoreGitProcessingOrder {
  public static List<EntityType> getEntityProcessingOrder() {
    return Lists.newArrayList(EntityType.PROJECTS, EntityType.SECRETS, EntityType.CONNECTORS);
  }
}
