package io.harness.enforcement.executions;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.service.PMSExecutionService;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Duration;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.PIPELINE)
public class BuildRestrictionUsageImpl implements RestrictionUsageInterface<RateLimitRestrictionMetadataDTO> {
  @Inject PMSExecutionService pmsExecutionService;

  @Override
  public long getCurrentValue(String accountIdentifier, RateLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    long millisecondsToCount =
        Duration
            .of(restrictionMetadataDTO.getTimeUnit().getNumberOfUnits(), restrictionMetadataDTO.getTimeUnit().getUnit())
            .toMillis();
    Criteria criteria =
        Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.accountId).is(accountIdentifier);
    criteria.and(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.executedModules)
        .in(Lists.newArrayList(ModuleType.CI.name().toLowerCase()));
    criteria.and(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.startTs)
        .gte(System.currentTimeMillis() - millisecondsToCount);

    return pmsExecutionService.getCountOfExecutions(criteria);
  }
}
