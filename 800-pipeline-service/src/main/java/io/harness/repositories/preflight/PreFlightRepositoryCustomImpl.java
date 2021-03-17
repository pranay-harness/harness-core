package io.harness.repositories.preflight;

import io.harness.pms.preflight.entity.PreFlightEntity;

import org.springframework.data.mongodb.core.query.Criteria;

public class PreFlightRepositoryCustomImpl implements PreFlightRepositoryCustom {
  @Override
  public PreFlightEntity update(Criteria criteria, PreFlightEntity entity) {
    return null;
  }
}
