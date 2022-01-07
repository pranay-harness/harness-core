/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.pcf.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.pcf.CfCommandResponse;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CDP)
public class CfInstanceSyncResponse extends CfCommandResponse {
  private String name;
  private String guid;
  private String organization;
  private String space;
  private List<String> instanceIndices;

  @Builder
  public CfInstanceSyncResponse(CommandExecutionStatus commandExecutionStatus, String output, String name, String guid,
      List<String> instanceIndicesx, String organization, String space) {
    super(commandExecutionStatus, output);
    this.name = name;
    this.guid = guid;
    this.instanceIndices = instanceIndicesx;
    this.organization = organization;
    this.space = space;
  }
}
