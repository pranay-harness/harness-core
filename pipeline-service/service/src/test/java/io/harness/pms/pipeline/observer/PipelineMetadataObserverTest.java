/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.observer;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.events.PipelineDeleteEvent;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PipelineMetadataService;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class PipelineMetadataObserverTest extends CategoryTest {
  @Mock PipelineMetadataService pipelineMetadataService;
  @InjectMocks PipelineMetadataObserver pipelineMetadataObserver;

  static final String ACCOUNT_ID = "account_id";
  static final String ORG_IDENTIFIER = "orgId";
  static final String PROJ_IDENTIFIER = "projId";
  static final String PIPE_IDENTIFIER = "pipelineIdentifier";

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetMetadataForGivenPipelineIds() {
    PipelineDeleteEvent pipelineDeleteEvent = new PipelineDeleteEvent(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER,
        PipelineEntity.builder()
            .accountId(ACCOUNT_ID)
            .orgIdentifier(ORG_IDENTIFIER)
            .projectIdentifier(PROJ_IDENTIFIER)
            .identifier(PIPE_IDENTIFIER)
            .build());
    pipelineMetadataObserver.onDelete(pipelineDeleteEvent);
    verify(pipelineMetadataService, times(1))
        .deletePipelineMetadata(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPE_IDENTIFIER);
  }
}