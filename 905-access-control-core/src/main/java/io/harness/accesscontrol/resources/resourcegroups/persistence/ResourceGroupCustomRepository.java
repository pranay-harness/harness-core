package io.harness.accesscontrol.resources.resourcegroups.persistence;

import io.harness.annotation.HarnessRepo;

import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;

@HarnessRepo
public interface ResourceGroupCustomRepository {
  List<ResourceGroupDBO> findAllWithCriteria(Criteria criteria);
}
