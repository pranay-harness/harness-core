/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.ChangeSets;
import io.harness.gitsync.ProcessingResponse;

import java.util.List;

/**
 * Implement this interface if you want a handle pre and post processing operation.
 */
@OwnedBy(DX)
public interface ChangeSetInterceptorService {
  void onChangeSetReceive(ChangeSets changeSets, String accountId);

  void postChangeSetProcessing(ProcessingResponse fileProcessingResponse, String accountId);

  void postChangeSetSort(List<ChangeSet> changeSetList, String accountId);
}
