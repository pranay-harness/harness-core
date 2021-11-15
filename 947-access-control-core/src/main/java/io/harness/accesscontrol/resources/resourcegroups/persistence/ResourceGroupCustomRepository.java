package io.harness.accesscontrol.resources.resourcegroups.persistence;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.PL)
@HarnessRepo
public interface ResourceGroupCustomRepository {
  List<ResourceGroupDBO> findAllWithCriteria(Criteria criteria);
  Optional<ResourceGroupDBO> find(Criteria criteria);
}
