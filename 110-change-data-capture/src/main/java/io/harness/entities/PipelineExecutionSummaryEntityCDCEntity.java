package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.changehandlers.PlanExecutionSummaryChangeDataHandler;
import io.harness.persistence.PersistentEntity;

import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import software.wings.beans.Application;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PipelineExecutionSummaryEntityCDCEntity implements CDCEntity<PipelineExecutionSummaryEntity> {
  @Inject private PlanExecutionSummaryChangeDataHandler planExecutionSummaryChangeDataHandler;

  @Override
  public ChangeHandler getTimescaleChangeHandler() {
    return planExecutionSummaryChangeDataHandler;
  }

  @Override
  public Class<? extends PersistentEntity> getSubscriptionEntity() {
    return PipelineExecutionSummaryEntity.class;
  }
}
