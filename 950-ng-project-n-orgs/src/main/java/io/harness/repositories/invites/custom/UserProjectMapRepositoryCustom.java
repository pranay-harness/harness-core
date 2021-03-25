package io.harness.repositories.invites.custom;

import io.harness.ng.core.invites.entities.UserProjectMap;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface UserProjectMapRepositoryCustom {
  List<UserProjectMap> findAll(Criteria criteria);

  Page<UserProjectMap> findAll(Criteria criteria, Pageable pageable);
}
