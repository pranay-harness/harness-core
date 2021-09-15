/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.EntityType;
import software.wings.beans.SubEntityType;
import software.wings.sm.StateType;

import java.util.Set;

/**
 * Created by sgurubelli on 8/7/17.
 */
@OwnedBy(CDC)
public interface ExpressionBuilderService {
  Set<String> listExpressions(String appId, String entityId, EntityType entityType);
  Set<String> listExpressions(String appId, String entityId, EntityType entityType, String serviceId);
  Set<String> listExpressions(
      String appId, String entityId, EntityType entityType, String serviceId, StateType stateType);

  Set<String> listExpressions(String appId, String entityId, EntityType entityType, String serviceId,
      StateType stateType, SubEntityType subEntityType, boolean forTags);

  Set<String> listExpressionsFromValuesForService(String appId, String serviceId);
}
