/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.plan.execution.handlers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.pipeline.ExecutionSummaryInfo;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.rule.Owner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@OwnedBy(PIPELINE)
public class ExecutionInfoUpdateEventHandlerTest extends PipelineServiceTestBase {
  @Mock private PMSPipelineService pmsPipelineService;
  @Mock private PlanExecutionService planExecutionService;

  private ExecutionInfoUpdateEventHandler executionInfoUpdateEventHandler;

  @Before
  public void setUp() {
    executionInfoUpdateEventHandler = new ExecutionInfoUpdateEventHandler(pmsPipelineService, planExecutionService);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestOnPlanStatusUpdate() {
    Ambiance ambiance = Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build();
    PipelineEntity pipelineEntity = PipelineEntity.builder()
                                        .uuid(generateUuid())
                                        .executionSummaryInfo(ExecutionSummaryInfo.builder()
                                                                  .lastExecutionStatus(ExecutionStatus.RUNNING)
                                                                  .numOfErrors(new HashMap<>())
                                                                  .build())
                                        .build();

    when(pmsPipelineService.get(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
        .thenReturn(Optional.of(pipelineEntity));

    when(planExecutionService.get(anyString())).thenReturn(PlanExecution.builder().status(Status.FAILED).build());

    ArgumentCaptor<ExecutionSummaryInfo> captor = ArgumentCaptor.forClass(ExecutionSummaryInfo.class);
    doNothing()
        .when(pmsPipelineService)
        .saveExecutionInfo(anyString(), anyString(), anyString(), anyString(), captor.capture());

    executionInfoUpdateEventHandler.onPlanStatusUpdate(ambiance);

    ExecutionSummaryInfo value = captor.getValue();
    assertThat(value.getLastExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(value.getNumOfErrors()).isNotEmpty();

    assertThat(value.getNumOfErrors().get(getFormattedDate())).isEqualTo(1);
  }

  private String getFormattedDate() {
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    return formatter.format(date);
  }
}
