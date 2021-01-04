package io.harness.repositories.invites.custom;

import io.harness.ng.core.invites.entities.UserProjectMap;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class UserProjectMapRepositoryCustomImpl implements UserProjectMapRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public List<UserProjectMap> findAll(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, UserProjectMap.class);
  }
}
