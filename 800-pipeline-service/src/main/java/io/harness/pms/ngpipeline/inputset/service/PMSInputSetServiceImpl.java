package io.harness.pms.ngpipeline.inputset.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.exception.WingsException.USER_SRE;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.repositories.inputset.PMSInputSetRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import java.util.Optional;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class PMSInputSetServiceImpl implements PMSInputSetService {
  @Inject private PMSInputSetRepository inputSetRepository;

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Input set [%s] under Project[%s], Organization [%s] for Pipeline [%s] already exists";

  @Override
  public InputSetEntity create(InputSetEntity inputSetEntity) {
    try {
      return inputSetRepository.save(inputSetEntity);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          format(DUP_KEY_EXP_FORMAT_STRING, inputSetEntity.getIdentifier(), inputSetEntity.getProjectIdentifier(),
              inputSetEntity.getOrgIdentifier(), inputSetEntity.getPipelineIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<InputSetEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String identifier, boolean deleted) {
    return inputSetRepository
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndIdentifierAndDeletedNot(
            accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier, !deleted);
  }

  @Override
  public InputSetEntity update(InputSetEntity inputSetEntity) {
    Criteria criteria = getInputSetEqualityCriteria(inputSetEntity, inputSetEntity.getDeleted());
    InputSetEntity updatedEntity = inputSetRepository.update(criteria, inputSetEntity);
    if (updatedEntity == null) {
      throw new InvalidRequestException(format(
          "Input Set [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
          inputSetEntity.getIdentifier(), inputSetEntity.getProjectIdentifier(), inputSetEntity.getOrgIdentifier()));
    }
    return updatedEntity;
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String identifier, Long version) {
    Criteria criteria = getInputSetEqualityCriteria(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier, false, version);
    UpdateResult updateResult = inputSetRepository.delete(criteria);
    if (!updateResult.wasAcknowledged() || updateResult.getModifiedCount() != 1) {
      throw new InvalidRequestException(
          format("Input Set [%s] under Project[%s], Organization [%s] couldn't be deleted.", identifier,
              projectIdentifier, orgIdentifier));
    }
    return true;
  }

  @Override
  public Page<InputSetEntity> list(Criteria criteria, Pageable pageable) {
    return inputSetRepository.findAll(criteria, pageable);
  }

  private Criteria getInputSetEqualityCriteria(@Valid InputSetEntity reqInputSet, boolean deleted) {
    return getInputSetEqualityCriteria(reqInputSet.getAccountId(), reqInputSet.getOrgIdentifier(),
        reqInputSet.getProjectIdentifier(), reqInputSet.getPipelineIdentifier(), reqInputSet.getIdentifier(), deleted,
        reqInputSet.getVersion());
  }

  private Criteria getInputSetEqualityCriteria(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String identifier, boolean deleted, Long version) {
    Criteria criteria = Criteria.where(InputSetEntityKeys.accountId)
                            .is(accountId)
                            .and(InputSetEntityKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(InputSetEntityKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(InputSetEntityKeys.pipelineIdentifier)
                            .is(pipelineIdentifier)
                            .and(InputSetEntityKeys.identifier)
                            .is(identifier)
                            .and(InputSetEntityKeys.deleted)
                            .is(deleted);

    if (version != null) {
      criteria.and(InputSetEntityKeys.version).is(version);
    }

    return criteria;
  }

  @Override
  public void deleteInputSetsOnPipelineDeletion(PipelineEntity pipelineEntity) {
    Criteria criteria = new Criteria();
    criteria.and(InputSetEntityKeys.accountId)
        .is(pipelineEntity.getAccountId())
        .and(InputSetEntityKeys.orgIdentifier)
        .is(pipelineEntity.getOrgIdentifier())
        .and(InputSetEntityKeys.projectIdentifier)
        .is(pipelineEntity.getProjectIdentifier())
        .and(InputSetEntityKeys.pipelineIdentifier)
        .is(pipelineEntity.getIdentifier());
    Query query = new Query(criteria);

    Update update = new Update();
    update.set(InputSetEntityKeys.deleted, Boolean.TRUE);

    UpdateResult updateResult = inputSetRepository.deleteAllInputSetsWhenPipelineDeleted(query, update);
    if (!updateResult.wasAcknowledged()) {
      throw new InvalidRequestException(format(
          "InputSets for Pipeline [%s] under Project[%s], Organization [%s] couldn't be deleted.",
          pipelineEntity.getIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getOrgIdentifier()));
    }
  }
}
