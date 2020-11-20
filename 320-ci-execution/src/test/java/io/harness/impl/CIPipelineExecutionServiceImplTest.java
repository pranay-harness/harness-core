package io.harness.impl;

import static io.harness.pms.execution.Status.RUNNING;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.engine.OrchestrationService;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.CIExecutionPlanCreatorRegistrar;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class CIPipelineExecutionServiceImplTest extends CIExecutionTest {
  @Mock private OrchestrationService orchestrationService;
  @Inject CIPipelineExecutionService ciPipelineExecutionService;
  @Inject CIExecutionPlanTestHelper executionPlanTestHelper;
  @Inject private CIExecutionPlanCreatorRegistrar ciExecutionPlanCreatorRegistrar;

  @Before
  public void setUp() {
    ciExecutionPlanCreatorRegistrar.register();
    on(ciPipelineExecutionService).set("orchestrationService", orchestrationService);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void executePipeline() {
    NgPipelineEntity ngPipelineEntity = executionPlanTestHelper.getCIPipeline();

    when(orchestrationService.startExecution(any(), any()))
        .thenReturn(PlanExecution.builder().uuid("planId").status(RUNNING).build());

    PlanExecution planExecution = ciPipelineExecutionService.executePipeline(
        ngPipelineEntity, executionPlanTestHelper.getCIExecutionArgs(), null);
    assertThat(planExecution).isNotNull();
    verify(orchestrationService, times(1)).startExecution(any(), any());
  }
}
