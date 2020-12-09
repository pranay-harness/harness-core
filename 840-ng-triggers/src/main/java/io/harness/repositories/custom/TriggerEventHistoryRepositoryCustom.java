package io.harness.repositories.ng.core.custom;

import io.harness.ngtriggers.beans.entity.TriggerEventHistory;

import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;

public interface TriggerEventHistoryRepositoryCustom {
  List<TriggerEventHistory> findAll(Criteria criteria);
  TriggerEventHistory findLatest(Criteria criteria);
}