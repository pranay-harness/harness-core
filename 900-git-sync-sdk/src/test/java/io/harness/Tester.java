/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SampleBean;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;
import io.harness.repositories.TestRepository;

import com.google.inject.Inject;

@OwnedBy(DX)
public class Tester {
  @Inject TestRepository cdRepository;
  @Inject HarnessToGitPushInfoServiceBlockingStub harnessToGitPushInfoServiceBlockingStub;

  public SampleBean save() {
    return cdRepository.save(SampleBean.builder()
                                 .accountIdentifier("kmpySmUISimoRrJL6NL73w")
                                 .projectIdentifier("test_cd")
                                 .orgIdentifier("test_org")
                                 .name("xyz")
                                 .identifier("id")
                                 .build());
  }
}
