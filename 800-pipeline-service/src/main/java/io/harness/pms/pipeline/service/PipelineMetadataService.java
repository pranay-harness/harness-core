/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.service;

import io.harness.pms.pipeline.PipelineMetadata;

import com.google.protobuf.ByteString;
import java.util.Optional;

public interface PipelineMetadataService {
  int incrementExecutionCounter(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, ByteString gitSyncBranchContext);

  PipelineMetadata save(PipelineMetadata metadata);

  Optional<PipelineMetadata> getMetadata(String accountId, String orgIdentifier, String projectIdentifier,
      String identifier, ByteString gitSyncBranchContext);
}
