package io.harness.accesscontrol.principals.usergroups.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
@HarnessRepo
public interface UserGroupCustomRepository {
  List<UserGroupDBO> findAllWithCriteria(Criteria criteria);
}
