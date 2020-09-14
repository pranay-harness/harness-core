package io.harness.ng.core.invites.repositories.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import com.mongodb.client.result.UpdateResult;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.RetryUtils;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Invite.InviteKeys;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.time.Duration;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class InviteRepositoryCustomImpl implements InviteRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  private final RetryPolicy<Object> updateRetryPolicy = RetryUtils.getRetryPolicy(
      "[Retrying]: Failed updating Invite; attempt: {}", "[Failed]: Failed updating Invite; attempt: {}",
      ImmutableList.of(OptimisticLockingFailureException.class, DuplicateKeyException.class), Duration.ofSeconds(1), 3,
      logger);

  @Override
  public Page<Invite> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Invite> invites = mongoTemplate.find(query, Invite.class);
    return PageableExecutionUtils.getPage(
        invites, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Invite.class));
  }

  @Override
  public UpdateResult updateInvite(String inviteId, Update update) {
    Criteria criteria = Criteria.where(InviteKeys.id).is(inviteId);
    Query query = new Query(criteria);

    return Failsafe.with(updateRetryPolicy).get(() -> mongoTemplate.updateFirst(query, update, Invite.class));
  }
}
