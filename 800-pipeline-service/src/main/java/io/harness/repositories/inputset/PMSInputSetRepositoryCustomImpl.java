/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.inputset;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxService;
import io.harness.pms.events.InputSetCreateEvent;
import io.harness.pms.events.InputSetDeleteEvent;
import io.harness.pms.events.InputSetUpdateEvent;
import io.harness.pms.inputset.gitsync.InputSetYamlDTO;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PMSInputSetRepositoryCustomImpl implements PMSInputSetRepositoryCustom {
  private final GitAwarePersistence gitAwarePersistence;
  private final MongoTemplate mongoTemplate;
  private final OutboxService outboxService;
  private final GitSyncSdkService gitSyncSdkService;

  @Override
  public List<InputSetEntity> findAll(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, InputSetEntity.class);
  }

  @Override
  public Page<InputSetEntity> findAll(
      Criteria criteria, Pageable pageable, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<InputSetEntity> inputSetEntities = gitAwarePersistence.find(
        criteria, pageable, projectIdentifier, orgIdentifier, accountIdentifier, InputSetEntity.class);

    return PageableExecutionUtils.getPage(inputSetEntities, pageable,
        ()
            -> gitAwarePersistence.count(
                criteria, projectIdentifier, orgIdentifier, accountIdentifier, InputSetEntity.class));
  }

  @Override
  public InputSetEntity save(InputSetEntity entityToSave, InputSetYamlDTO yamlDTO) {
    Supplier<OutboxEvent> functor = ()
        -> outboxService.save(InputSetCreateEvent.builder()
                                  .accountIdentifier(entityToSave.getAccountIdentifier())
                                  .orgIdentifier(entityToSave.getOrgIdentifier())
                                  .projectIdentifier(entityToSave.getProjectIdentifier())
                                  .pipelineIdentifier(entityToSave.getPipelineIdentifier())
                                  .inputSet(entityToSave)
                                  .build());
    return gitAwarePersistence.save(
        entityToSave, entityToSave.getYaml(), ChangeType.ADD, InputSetEntity.class, functor);
  }

  @Override
  public Optional<InputSetEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndIdentifierAndDeletedNot(String accountId,
      String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String identifier,
      boolean notDeleted) {
    return gitAwarePersistence.findOne(Criteria.where(InputSetEntityKeys.deleted)
                                           .is(!notDeleted)
                                           .and(InputSetEntityKeys.accountId)
                                           .is(accountId)
                                           .and(InputSetEntityKeys.orgIdentifier)
                                           .is(orgIdentifier)
                                           .and(InputSetEntityKeys.projectIdentifier)
                                           .is(projectIdentifier)
                                           .and(InputSetEntityKeys.pipelineIdentifier)
                                           .is(pipelineIdentifier)
                                           .and(InputSetEntityKeys.identifier)
                                           .is(identifier),
        projectIdentifier, orgIdentifier, accountId, InputSetEntity.class);
  }

  @Override
  public InputSetEntity update(InputSetEntity entityToUpdate, InputSetYamlDTO yamlDTO, ChangeType changeType) {
    Supplier<OutboxEvent> functor = null;
    if (!gitSyncSdkService.isGitSyncEnabled(entityToUpdate.getAccountIdentifier(), entityToUpdate.getOrgIdentifier(),
            entityToUpdate.getProjectIdentifier())) {
      Optional<InputSetEntity> inputSetEntityOptional =
          findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndIdentifierAndDeletedNot(
              entityToUpdate.getAccountIdentifier(), entityToUpdate.getOrgIdentifier(),
              entityToUpdate.getProjectIdentifier(), entityToUpdate.getPipelineIdentifier(),
              entityToUpdate.getIdentifier(), true);
      if (inputSetEntityOptional.isPresent()) {
        InputSetEntity oldInputSet = inputSetEntityOptional.get();
        functor = ()
            -> outboxService.save(InputSetUpdateEvent.builder()
                                      .accountIdentifier(entityToUpdate.getAccountIdentifier())
                                      .orgIdentifier(entityToUpdate.getOrgIdentifier())
                                      .projectIdentifier(entityToUpdate.getProjectIdentifier())
                                      .pipelineIdentifier(entityToUpdate.getPipelineIdentifier())
                                      .newInputSet(entityToUpdate)
                                      .oldInputSet(oldInputSet)
                                      .build());
      } else {
        throw new InvalidRequestException("No such input set exist");
      }
    }

    return gitAwarePersistence.save(
        entityToUpdate, entityToUpdate.getYaml(), changeType, InputSetEntity.class, functor);
  }

  @Override
  public InputSetEntity switchValidationFlag(Criteria criteria, Update update) {
    Query query = new Query(criteria);
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed updating Input Set; attempt: {}", "[Failed]: Failed updating Input Set; attempt: {}");
    return Failsafe.with(retryPolicy)
        .get(()
                 -> mongoTemplate.findAndModify(
                     query, update, new FindAndModifyOptions().returnNew(true), InputSetEntity.class));
  }

  @Override
  public InputSetEntity delete(InputSetEntity entityToDelete, InputSetYamlDTO yamlDTO) {
    Supplier<OutboxEvent> functor = ()
        -> outboxService.save(InputSetDeleteEvent.builder()
                                  .accountIdentifier(entityToDelete.getAccountIdentifier())
                                  .orgIdentifier(entityToDelete.getOrgIdentifier())
                                  .projectIdentifier(entityToDelete.getProjectIdentifier())
                                  .pipelineIdentifier(entityToDelete.getPipelineIdentifier())
                                  .inputSet(entityToDelete)
                                  .build());
    return gitAwarePersistence.save(
        entityToDelete, entityToDelete.getYaml(), ChangeType.DELETE, InputSetEntity.class, functor);
  }

  @Override
  public UpdateResult deleteAllInputSetsWhenPipelineDeleted(Query query, Update update) {
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying]: Failed deleting Input Set; attempt: {}", "[Failed]: Failed deleting Input Set; attempt: {}");
    return Failsafe.with(retryPolicy).get(() -> mongoTemplate.updateMulti(query, update, InputSetEntity.class));
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
    int MAX_ATTEMPTS = 3;
    return new RetryPolicy<>()
        .handle(OptimisticLockingFailureException.class)
        .handle(DuplicateKeyException.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
