package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.changehandlers.PlanExecutionSummaryCdChangeDataHandler;
import io.harness.changehandlers.PlanExecutionSummaryCdServiceAndInfraChangeDataHandler;
import io.harness.changehandlers.PlanExecutionSummaryChangeDataHandler;
import io.harness.persistence.PersistentEntity;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CI)
@Slf4j
public class PipelineExecutionSummaryEntityCDCEntity implements CDCEntity<PipelineExecutionSummaryEntity> {
  @Inject private PlanExecutionSummaryChangeDataHandler planExecutionSummaryChangeDataHandler;
  @Inject private PlanExecutionSummaryCdChangeDataHandler planExecutionSummaryCdChangeDataHandler;
  @Inject
  private PlanExecutionSummaryCdServiceAndInfraChangeDataHandler planExecutionSummaryCdServiceAndInfraChangeDataHandler;
  @Override
  public ChangeHandler getChangeHandler(String handlerClass) {
    if (handlerClass.contentEquals("PipelineExecutionSummaryEntity")) {
      return planExecutionSummaryChangeDataHandler;
    } else if (handlerClass.contentEquals("PipelineExecutionSummaryEntityCD")) {
      return planExecutionSummaryCdChangeDataHandler;
    } else if (handlerClass.contentEquals("PipelineExecutionSummaryEntityServiceAndInfra")) {
      return planExecutionSummaryCdServiceAndInfraChangeDataHandler;
    }
    return null;
  }

  @Override
  public Class<? extends PersistentEntity> getSubscriptionEntity() {
    return PipelineExecutionSummaryEntity.class;
  }
}