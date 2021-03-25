package io.harness.repositories.invites.custom;

import io.harness.ng.core.invites.entities.UserProjectMap;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class UserProjectMapRepositoryCustomImpl implements UserProjectMapRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public List<UserProjectMap> findAll(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, UserProjectMap.class);
  }

  @Override
  public Page<UserProjectMap> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<UserProjectMap> userProjectMaps = mongoTemplate.find(query, UserProjectMap.class);
    return PageableExecutionUtils.getPage(
        userProjectMaps, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), UserProjectMap.class));
  }
}
