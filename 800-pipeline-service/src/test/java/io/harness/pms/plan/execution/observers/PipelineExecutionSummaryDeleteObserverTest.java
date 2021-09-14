/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.plan.execution.observers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.SAMARTH;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(PIPELINE)
public class PipelineExecutionSummaryDeleteObserverTest extends PipelineServiceTestBase {
  @InjectMocks PipelineExecutionSummaryDeleteObserver pipelineExecutionSummaryDeleteObserver;
  @Mock PMSExecutionService pmsExecutionService;

  private static final String ACCOUNT_ID = "accountId";
  private static final String PROJECT_ID = "projectId";
  private static final String ORG_ID = "orgId";
  private static final String PIPELINE_ID = "pipelineId";

  PipelineEntity pipelineEntity;

  @Before
  public void setUp() {
    pipelineEntity = PipelineEntity.builder()
                         .accountId(ACCOUNT_ID)
                         .orgIdentifier(ORG_ID)
                         .projectIdentifier(PROJECT_ID)
                         .identifier(PIPELINE_ID)
                         .build();
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testOnDelete() {
    doNothing().when(pmsExecutionService).deleteExecutionsOnPipelineDeletion(pipelineEntity);
    pipelineExecutionSummaryDeleteObserver.onDelete(pipelineEntity);
    verify(pmsExecutionService, times(1)).deleteExecutionsOnPipelineDeletion(pipelineEntity);
  }
}
