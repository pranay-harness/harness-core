package io.harness.repositories.invites.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.UserMembership;

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
@OwnedBy(PL)
public class UserMembershipRepositoryCustomImpl implements UserMembershipRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public List<UserMembership> findAll(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, UserMembership.class);
  }

  @Override
  public Page<UserMembership> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<UserMembership> userMemberships = mongoTemplate.find(query, UserMembership.class);
    return PageableExecutionUtils.getPage(
        userMemberships, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), UserMembership.class));
  }
}
