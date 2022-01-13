/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
