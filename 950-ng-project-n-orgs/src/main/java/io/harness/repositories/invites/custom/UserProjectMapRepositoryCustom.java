package io.harness.repositories.invites.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.UserProjectMap;

import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface UserProjectMapRepositoryCustom {
  List<UserProjectMap> findAll(Criteria criteria);
}
