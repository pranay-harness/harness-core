package io.harness.repositories.entitysetupusage;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(DX)
public interface EntitySetupUsageCustomRepository {
  Page<EntitySetupUsage> findAll(Criteria criteria, Pageable pageable);

  long countAll(Criteria criteria);

  Boolean exists(Criteria criteria);

  long delete(Criteria criteria);
}
