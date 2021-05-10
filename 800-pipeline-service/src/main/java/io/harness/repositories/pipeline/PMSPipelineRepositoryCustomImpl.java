package io.harness.repositories.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitSyncableHarnessRepo;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;

import com.google.inject.Inject;
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
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@GitSyncableHarnessRepo
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PMSPipelineRepositoryCustomImpl implements PMSPipelineRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  private final GitAwarePersistence gitAwarePersistence;

  @Override
  public Page<PipelineEntity> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<PipelineEntity> projects = mongoTemplate.find(query, PipelineEntity.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), PipelineEntity.class));
  }

  @Override
  public PipelineEntity save(PipelineEntity pipelineToSave, PipelineConfig yamlDTO) {
    return gitAwarePersistence.save(pipelineToSave, yamlDTO, ChangeType.ADD, PipelineEntity.class);
  }

  @Override
  public Optional<PipelineEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, boolean notDeleted) {
    return gitAwarePersistence.findOne(Criteria.where(PipelineEntityKeys.deleted)
                                           .is(!notDeleted)
                                           .and(PipelineEntityKeys.identifier)
                                           .is(pipelineIdentifier)
                                           .and(PipelineEntityKeys.projectIdentifier)
                                           .is(projectIdentifier)
                                           .and(PipelineEntityKeys.orgIdentifier)
                                           .is(orgIdentifier)
                                           .and(PipelineEntityKeys.accountId)
                                           .is(accountId),
        projectIdentifier, orgIdentifier, accountId, PipelineEntity.class);
  }

  @Override
  public PipelineEntity updatePipelineYaml(PipelineEntity pipelineToUpdate, PipelineConfig yamlDTO) {
    return gitAwarePersistence.save(pipelineToUpdate, yamlDTO, ChangeType.MODIFY, PipelineEntity.class);
  }

  @Override
  public PipelineEntity updatePipelineMetadata(Criteria criteria, Update update) {
    Query query = new Query(criteria);
    RetryPolicy<Object> retryPolicy = getRetryPolicyForPipelineUpdate();
    return Failsafe.with(retryPolicy)
        .get(()
                 -> mongoTemplate.findAndModify(
                     query, update, new FindAndModifyOptions().returnNew(true), PipelineEntity.class));
  }

  @Override
  public PipelineEntity deletePipeline(PipelineEntity pipelineToUpdate, PipelineConfig yamlDTO) {
    return gitAwarePersistence.save(pipelineToUpdate, yamlDTO, ChangeType.DELETE, PipelineEntity.class);
  }

  private RetryPolicy<Object> getRetryPolicyForPipelineUpdate() {
    int MAX_ATTEMPTS = 3;
    Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
    return new RetryPolicy<>()
        .handle(OptimisticLockingFailureException.class)
        .handle(DuplicateKeyException.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event
            -> log.info(
                "[Retrying]: Failed updating Pipeline; attempt: {}", event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event
            -> log.error(
                "[Failed]: Failed updating Pipeline; attempt: {}", event.getAttemptCount(), event.getFailure()));
  }
}
