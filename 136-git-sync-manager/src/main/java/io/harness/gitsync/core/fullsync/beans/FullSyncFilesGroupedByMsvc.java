/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.fullsync.beans;

import io.harness.Microservice;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FullSyncFilesGroupedByMsvc {
  Microservice microservice;
  List<GitFullSyncEntityInfo> gitFullSyncEntityInfoList;
  ;
}
