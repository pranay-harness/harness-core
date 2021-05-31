package io.harness.repositories.inputset;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.pms.inputset.gitsync.InputSetYamlDTO;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
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

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PMSInputSetRepositoryCustomImpl implements PMSInputSetRepositoryCustom {
  private final GitAwarePersistence gitAwarePersistence;
  private final MongoTemplate mongoTemplate;

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
    return gitAwarePersistence.save(entityToSave, yamlDTO, ChangeType.ADD, InputSetEntity.class);
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
  public InputSetEntity update(InputSetEntity entityToUpdate, InputSetYamlDTO yamlDTO) {
    return gitAwarePersistence.save(entityToUpdate, yamlDTO, ChangeType.MODIFY, InputSetEntity.class);
  }

  @Override
  public InputSetEntity delete(InputSetEntity entityToDelete, InputSetYamlDTO yamlDTO) {
    return gitAwarePersistence.save(entityToDelete, yamlDTO, ChangeType.DELETE, InputSetEntity.class);
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
