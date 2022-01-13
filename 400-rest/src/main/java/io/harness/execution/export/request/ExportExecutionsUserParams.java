/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
