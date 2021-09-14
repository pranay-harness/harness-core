/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
