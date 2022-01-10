/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.service;

import io.harness.pms.pipeline.PipelineMetadata;
import io.harness.repositories.pipeline.PipelineMetadataRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.util.Optional;

@Singleton
public class PipelineMetadataServiceImpl implements PipelineMetadataService {
  @Inject private PipelineMetadataRepository pipelineMetadataRepository;

  @Override
  public int incrementExecutionCounter(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, ByteString gitSyncBranchContext) {
    return pipelineMetadataRepository
        .incCounter(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, gitSyncBranchContext)
        .getRunSequence();
  }

  @Override
  public PipelineMetadata save(PipelineMetadata metadata) {
    return pipelineMetadataRepository.save(metadata);
  }

  @Override
  public Optional<PipelineMetadata> getMetadata(String accountId, String orgIdentifier, String projectIdentifier,
      String identifier, ByteString gitSyncBranchContext) {
    return pipelineMetadataRepository.getPipelineMetadata(
        accountId, orgIdentifier, projectIdentifier, identifier, gitSyncBranchContext);
  }
}
