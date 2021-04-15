package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.changehandlers.PlanExecutionSummaryChangeDataHandler;
import io.harness.persistence.PersistentEntity;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CI)
@Slf4j
public class PipelineExecutionSummaryEntityCDCEntity implements CDCEntity<PipelineExecutionSummaryEntity> {
  @Inject private PlanExecutionSummaryChangeDataHandler planExecutionSummaryChangeDataHandler;

  @Override
  public ChangeHandler getTimescaleChangeHandler(String handlerClass) {
    return planExecutionSummaryChangeDataHandler;
  }

  @Override
  public Class<? extends PersistentEntity> getSubscriptionEntity() {
    return PipelineExecutionSummaryEntity.class;
  }
}