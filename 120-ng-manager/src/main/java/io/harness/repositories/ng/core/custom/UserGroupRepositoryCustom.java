package io.harness.repositories.ng.core.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entities.UserGroup;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface UserGroupRepositoryCustom {
  Optional<UserGroup> find(Criteria criteria);

  Page<UserGroup> findAll(Criteria criteria, Pageable pageable);

  UserGroup delete(Criteria criteria);
}
