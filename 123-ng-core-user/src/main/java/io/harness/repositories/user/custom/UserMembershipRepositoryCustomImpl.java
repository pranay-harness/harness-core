package io.harness.repositories.user.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope.ScopeKeys;
import io.harness.ng.core.invites.dto.UserMetadataDTO;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
@OwnedBy(PL)
@Slf4j
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

  @Override
  public Page<String> findAllUserIds(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    query.fields().include(UserMembershipKeys.userId);
    List<UserMembership> userMemberships = mongoTemplate.find(query, UserMembership.class);
    List<String> userIds = userMemberships.stream().map(UserMembership::getUserId).collect(Collectors.toList());
    return PageableExecutionUtils.getPage(
        userIds, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), UserMembership.class));
  }

  @Override
  public List<UserMetadataDTO> getUserMetadata(Criteria criteria) {
    Query query = new Query(criteria);
    query.fields()
        .include(UserMembershipKeys.userId)
        .include(UserMembershipKeys.emailId)
        .include(UserMembershipKeys.name);
    List<UserMembership> userMemberships = mongoTemplate.find(query, UserMembership.class);
    return userMemberships.stream()
        .map(userMembership
            -> UserMetadataDTO.builder()
                   .uuid(userMembership.getUserId())
                   .email(userMembership.getEmailId())
                   .name(userMembership.getName())
                   .build())
        .collect(Collectors.toList());
  }

  @Override
  public UserMembership update(String userId, Update update) {
    Criteria criteria = Criteria.where(UserMembershipKeys.userId).is(userId);
    Query query = new Query(criteria);
    return mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), UserMembership.class);
  }

  @Override
  public Set<String> filterUsersWithMembership(List<String> userIds, String accountIdentifier,
      @Nullable String orgIdentifier, @Nullable String projectIdentifier) {
    Query query = new Query();
    query.addCriteria(where(UserMembershipKeys.userId).in(userIds));
    query.addCriteria(where(UserMembershipKeys.scopes)
                          .elemMatch(where(ScopeKeys.accountIdentifier)
                                         .is(accountIdentifier)
                                         .and(ScopeKeys.orgIdentifier)
                                         .is(orgIdentifier)
                                         .and(ScopeKeys.projectIdentifier)
                                         .is(projectIdentifier)));
    query.fields().include(UserMembershipKeys.userId);
    List<UserMembership> userMemberships = mongoTemplate.find(query, UserMembership.class);
    return userMemberships.stream().map(UserMembership::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
  }

  @Override
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
