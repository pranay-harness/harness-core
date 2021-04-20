package io.harness.repositories.preflight;

import io.harness.pms.preflight.entity.PreFlightEntity;

import org.springframework.data.mongodb.core.query.Criteria;

public interface PreFlightRepositoryCustom {
  PreFlightEntity update(Criteria criteria, PreFlightEntity entity);
}
