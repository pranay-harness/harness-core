package io.harness.repositories.custom;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecution;

import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
public interface PlanExecutionRepositoryCustom {
  List<PlanExecution> findAll(Criteria criteria);
}
