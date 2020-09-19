package io.harness.ng.gitsync;

import com.google.common.collect.Lists;

import io.harness.EntityType;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class NgCoreGitProcessingOrder {
  public static List<EntityType> getEntityProcessingOrder() {
    return Lists.newArrayList(EntityType.PROJECTS, EntityType.SECRETS, EntityType.CONNECTORS);
  }
}
