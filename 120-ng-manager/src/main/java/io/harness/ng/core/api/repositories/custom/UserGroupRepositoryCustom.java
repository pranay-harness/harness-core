package io.harness.ng.core.api.repositories.custom;

import io.harness.ng.core.entities.UserGroup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface UserGroupRepositoryCustom {
  Page<UserGroup> findAll(Criteria criteria, Pageable pageable);
}
