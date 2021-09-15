/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.mutation.execution.input;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(HarnessTeam.CDC)
public enum QLManifestInputType implements QLEnum {
  HELM_CHART_ID,
  VERSION_NUMBER;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
