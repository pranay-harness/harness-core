package io.harness.repositories.custom;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory;

import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
public interface TriggerEventHistoryRepositoryCustom {
  List<TriggerEventHistory> findAll(Criteria criteria);
  List<TriggerEventHistory> findAllActivationTimestampsInRange(Criteria criteria);
}