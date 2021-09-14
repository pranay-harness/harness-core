/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.ChangeSets;
import io.harness.gitsync.ProcessingResponse;

import java.util.List;

@OwnedBy(DX)
public class NoOpChangeSetInterceptorServiceImpl implements ChangeSetInterceptorService {
  @Override
  public void onChangeSetReceive(ChangeSets changeSets, String accountId) {}

  @Override
  public void postChangeSetProcessing(ProcessingResponse fileProcessingResponse, String accountId) {}

  @Override
  public void postChangeSetSort(List<ChangeSet> changeSetList, String accountId) {}
}
