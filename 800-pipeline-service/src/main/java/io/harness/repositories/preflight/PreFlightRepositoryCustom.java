package io.harness.repositories.preflight;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.preflight.entity.PreFlightEntity;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PreFlightRepositoryCustom {
  PreFlightEntity update(Criteria criteria, PreFlightEntity entity);

  PreFlightEntity update(Criteria criteria, Update update);
}
