/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.mutation.execution.export;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.execution.export.request.ExportExecutionsRequest.Status;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLExportExecutionsStatus implements QLEnum {
  QUEUED,
  READY,
  FAILED,
  EXPIRED;

  @Override
  public String getStringValue() {
    return this.name();
  }

  public static QLExportExecutionsStatus fromStatus(Status status) {
    if (status == null) {
      return null;
    }

    if (status == Status.QUEUED) {
      return QUEUED;
    } else if (status == Status.READY) {
      return READY;
    } else if (status == Status.FAILED) {
      return FAILED;
    } else if (status == Status.EXPIRED) {
      return EXPIRED;
    }

    return null;
  }
}
