/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.helm;

import io.harness.logging.CommandExecutionStatus;

import software.wings.helpers.ext.helm.response.ReleaseInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
public class HelmReleaseHistoryCmdResponseNG extends HelmCommandResponseNG {
  List<ReleaseInfo> releaseInfoList;

  @Builder
  public HelmReleaseHistoryCmdResponseNG(
      CommandExecutionStatus commandExecutionStatus, String output, List<ReleaseInfo> releaseInfoList) {
    super(commandExecutionStatus, output);
    this.releaseInfoList = releaseInfoList;
  }
}
