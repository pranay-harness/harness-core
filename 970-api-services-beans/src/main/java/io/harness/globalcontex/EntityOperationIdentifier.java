/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.globalcontex;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PL)
public class EntityOperationIdentifier {
  public enum EntityOperation { CREATE, UPDATE, DELETE }

  private String entityType;
  private String entityName;
  private String entityId;
  private EntityOperation operation;

  @Override
  public String toString() {
    return new StringBuilder(128).append(entityType).append(entityName).append(entityId).append(operation).toString();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof EntityOperationIdentifier)) {
      return false;
    }

    return toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
