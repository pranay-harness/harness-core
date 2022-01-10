/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.aggregation;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(CV)
public enum QLExecutionStatusType implements QLEnum {
  ABORTED,
  ERROR,
  FAILED,
  RUNNING,
  SUCCESS,
  SKIPPED,
  EXPIRED;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
