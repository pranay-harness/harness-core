package io.harness.impl;

import static io.harness.beans.executionargs.ExecutionArgs.EXEC_ARGS;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ci.beans.entities.CIBuild;
import io.harness.core.ci.services.CIBuildServiceImpl;
import io.harness.engine.OrchestrationService;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.plan.Plan;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Singleton
@Slf4j
public class CIPipelineExecutionServiceImpl implements CIPipelineExecutionService {
  @Inject private OrchestrationService orchestrationService;
  @Inject private ExecutionPlanCreatorService executionPlanCreatorService;
  @Inject private CIBuildServiceImpl ciBuildService;

  public PlanExecution executePipeline(
      NgPipelineEntity ngPipelineEntity, CIExecutionArgs ciExecutionArgs, Long buildNumber) {
    Map<String, Object> contextAttributes = new HashMap<>();
    contextAttributes.put(EXEC_ARGS, ciExecutionArgs);

    Plan plan = executionPlanCreatorService.createPlanForPipeline(
        ngPipelineEntity.getNgPipeline(), ngPipelineEntity.getAccountId(), contextAttributes);
    // TODO set user before execution which will be available once we build authentication
    // User user = UserThreadLocal.get()
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("accountId", ngPipelineEntity.getAccountId());
    setupAbstractions.put("userId", "harsh");
    setupAbstractions.put("userName", "harsh jain");
    setupAbstractions.put("userEmail", "harsh.jain@harness.io");

    PlanExecution planExecution = orchestrationService.startExecution(plan, setupAbstractions);
    createCIBuild(ngPipelineEntity, ciExecutionArgs, planExecution, buildNumber);
    return planExecution;
  }

  private CIBuild createCIBuild(NgPipelineEntity ngPipelineEntity, CIExecutionArgs ciExecutionArgs,
      PlanExecution planExecution, Long buildNumber) {
    CIBuild ciBuild = CIBuild.builder()
                          .buildNumber(buildNumber)
                          .orgIdentifier(ngPipelineEntity.getOrgIdentifier())
                          .executionSource(ciExecutionArgs.getExecutionSource())
                          .projectIdentifier(ngPipelineEntity.getProjectIdentifier())
                          .pipelineIdentifier(ngPipelineEntity.getIdentifier())
                          .inputSet(ciExecutionArgs.getInputSet())
                          .triggerTime(System.currentTimeMillis()) // TODO Generate time at entry point
                          .accountIdentifier(ngPipelineEntity.getAccountId())
                          .executionId(planExecution.getUuid())
                          .build();

    return ciBuildService.save(ciBuild);
  }
}
