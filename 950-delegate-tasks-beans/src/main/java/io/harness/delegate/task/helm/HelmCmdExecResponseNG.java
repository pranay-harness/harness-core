/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.helm;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.logging.CommandExecutionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.NonFinal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class HelmCmdExecResponseNG implements DelegateTaskNotifyResponseData {
  private HelmCommandResponseNG helmCommandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
  private UnitProgressData commandUnitsProgress;
  @NonFinal DelegateMetaInfo delegateMetaInfo;

  @Override
  public void setDelegateMetaInfo(DelegateMetaInfo metaInfo) {
    this.delegateMetaInfo = metaInfo;
  }
}
