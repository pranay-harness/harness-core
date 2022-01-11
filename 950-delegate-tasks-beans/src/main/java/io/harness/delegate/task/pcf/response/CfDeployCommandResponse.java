/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.pcf.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.pcf.CfInternalInstanceElement;
import io.harness.delegate.beans.pcf.CfServiceData;
import io.harness.delegate.task.pcf.CfCommandResponse;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CDP)
public class CfDeployCommandResponse extends CfCommandResponse {
  /**
   * This list represents apps updated by deploy state,
   * AppName : previousCount : DesiredCount (one updated by deploy)
   * Rollback will use this data but will reverse counts
   */
  private List<CfServiceData> instanceDataUpdated;
  private List<CfInternalInstanceElement> pcfInstanceElements;

  @Builder
  public CfDeployCommandResponse(CommandExecutionStatus commandExecutionStatus, String output,
      List<CfServiceData> instanceDataUpdated, List<CfInternalInstanceElement> pcfInstanceElements) {
    super(commandExecutionStatus, output);
    this.instanceDataUpdated = instanceDataUpdated;
    this.pcfInstanceElements = pcfInstanceElements;
  }
}
