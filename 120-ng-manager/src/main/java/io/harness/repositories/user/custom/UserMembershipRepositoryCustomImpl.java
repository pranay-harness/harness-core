package io.harness.repositories.user.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
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

  public Long getProjectCount(String userId) {
    TypedAggregation<UserMembership> aggregation = newAggregation(UserMembership.class,
        match(where(UserMembershipKeys.userId).is(userId)), unwind(UserMembershipKeys.scopes),
        match(where("scopes.projectIdentifier").exists(true)), group().count().as("count"));

    List<BasicDBObject> mappedResults = mongoTemplate.aggregate(aggregation, BasicDBObject.class).getMappedResults();

    return (isNotEmpty(mappedResults) && null != mappedResults.get(0) && null != mappedResults.get(0).get("count"))
        ? Long.parseLong(mappedResults.get(0).get("count").toString())
        : 0L;
  }
}
