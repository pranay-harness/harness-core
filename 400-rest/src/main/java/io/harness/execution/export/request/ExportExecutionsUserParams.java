/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.execution.export.request;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CreatedByType;
import io.harness.execution.export.request.ExportExecutionsRequest.OutputFormat;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@Data
@Builder
public class ExportExecutionsUserParams {
  @Builder.Default private OutputFormat outputFormat = OutputFormat.JSON;
  private boolean notifyOnlyTriggeringUser;
  private List<String> userGroupIds;
  private CreatedByType createdByType;
}
