package io.harness.ng.core.api.repositories.custom;

import static io.harness.ng.core.utils.NGUtils.getPaginatedResult;

import io.harness.ng.core.entities.UserGroup;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class UserGroupRepositoryCustomImpl implements UserGroupRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  public Page<UserGroup> findAll(Criteria criteria, Pageable pageable) {
    return getPaginatedResult(criteria, pageable, UserGroup.class, mongoTemplate);
  }
}
