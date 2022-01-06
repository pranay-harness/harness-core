/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.app.schema.type.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(DEL)
public enum QLDelegateStatus implements QLEnum {
  ENABLED,
  WAITING_FOR_APPROVAL,
  DELETED;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
