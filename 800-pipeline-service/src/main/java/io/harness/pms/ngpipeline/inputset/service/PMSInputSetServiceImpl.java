/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.ngpipeline.inputset.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.exception.WingsException.USER_SRE;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.InputSetReferenceProtoDTO;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.scm.EntityObjectIdUtils;
import io.harness.grpc.utils.StringValueUtils;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.inputset.gitsync.InputSetYamlDTOMapper;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.repositories.inputset.PMSInputSetRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import java.util.Optional;
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
      return inputSetRepository.save(inputSetEntity, InputSetYamlDTOMapper.toDTO(inputSetEntity));
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          format(DUP_KEY_EXP_FORMAT_STRING, inputSetEntity.getIdentifier(), inputSetEntity.getProjectIdentifier(),
              inputSetEntity.getOrgIdentifier(), inputSetEntity.getPipelineIdentifier()),
          USER_SRE, ex);
    } catch (Exception e) {
      log.error(String.format("Error while saving input set [%s]", inputSetEntity.getIdentifier()), e);
      throw new InvalidRequestException(
          String.format("Error while saving input set [%s]: %s", inputSetEntity.getIdentifier(), e.getMessage()));
    }
  }

  @Override
  public Optional<InputSetEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String identifier, boolean deleted) {
    try {
      return inputSetRepository
          .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndIdentifierAndDeletedNot(
              accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier, !deleted);
    } catch (Exception e) {
      log.error(String.format("Error while retrieving input set [%s]", identifier), e);
      throw new InvalidRequestException(
          String.format("Error while retrieving input set [%s]: %s", identifier, e.getMessage()));
    }
  }

  @Override
  public InputSetEntity update(InputSetEntity inputSetEntity, ChangeType changeType) {
    if (GitContextHelper.getGitEntityInfo() != null && GitContextHelper.getGitEntityInfo().isNewBranch()) {
      return makeInputSetUpdateCall(inputSetEntity, changeType);
    }
    Optional<InputSetEntity> optionalOriginalEntity =
        get(inputSetEntity.getAccountId(), inputSetEntity.getOrgIdentifier(), inputSetEntity.getProjectIdentifier(),
            inputSetEntity.getPipelineIdentifier(), inputSetEntity.getIdentifier(), false);
    if (!optionalOriginalEntity.isPresent()) {
      throw new InvalidRequestException(
          format("Input Set [%s], for pipeline [%s], under Project[%s], Organization [%s] doesn't exist.",
              inputSetEntity.getIdentifier(), inputSetEntity.getPipelineIdentifier(),
              inputSetEntity.getProjectIdentifier(), inputSetEntity.getOrgIdentifier()));
    }

    InputSetEntity originalEntity = optionalOriginalEntity.get();
    if (inputSetEntity.getVersion() != null && !inputSetEntity.getVersion().equals(originalEntity.getVersion())) {
      throw new InvalidRequestException(format(
          "Input Set [%s], for pipeline [%s], under Project[%s], Organization [%s] is not on the correct version.",
          inputSetEntity.getIdentifier(), inputSetEntity.getPipelineIdentifier(), inputSetEntity.getProjectIdentifier(),
          inputSetEntity.getOrgIdentifier()));
    }
    InputSetEntity entityToUpdate = originalEntity.withYaml(inputSetEntity.getYaml())
                                        .withName(inputSetEntity.getName())
                                        .withDescription(inputSetEntity.getDescription())
                                        .withTags(inputSetEntity.getTags())
                                        .withInputSetReferences(inputSetEntity.getInputSetReferences())
                                        .withIsInvalid(false)
                                        .withIsEntityInvalid(false);

    return makeInputSetUpdateCall(entityToUpdate, changeType);
  }

  @Override
  public InputSetEntity syncInputSetWithGit(EntityDetailProtoDTO entityDetail) {
    InputSetReferenceProtoDTO inputSetRef = entityDetail.getInputSetRef();
    String accountId = StringValueUtils.getStringFromStringValue(inputSetRef.getAccountIdentifier());
    String orgId = StringValueUtils.getStringFromStringValue(inputSetRef.getOrgIdentifier());
    String projectId = StringValueUtils.getStringFromStringValue(inputSetRef.getProjectIdentifier());
    String pipelineId = StringValueUtils.getStringFromStringValue(inputSetRef.getPipelineIdentifier());
    String inputSetId = StringValueUtils.getStringFromStringValue(inputSetRef.getIdentifier());
    Optional<InputSetEntity> optionalInputSetEntity;
    try (PmsGitSyncBranchContextGuard ignored = new PmsGitSyncBranchContextGuard(null, false)) {
      optionalInputSetEntity = get(accountId, orgId, projectId, pipelineId, inputSetId, false);
    }
    if (!optionalInputSetEntity.isPresent()) {
      throw new InvalidRequestException(
          format("Input Set [%s], for pipeline [%s], under Project[%s], Organization [%s] doesn't exist.", inputSetId,
              pipelineId, projectId, orgId));
    }
    return makeInputSetUpdateCall(optionalInputSetEntity.get(), ChangeType.ADD);
  }

  @Override
  public boolean switchValidationFlag(InputSetEntity entity, boolean isInvalid) {
    Criteria criteria = new Criteria();
    criteria.and(InputSetEntityKeys.accountId)
        .is(entity.getAccountId())
        .and(InputSetEntityKeys.orgIdentifier)
        .is(entity.getOrgIdentifier())
        .and(InputSetEntityKeys.projectIdentifier)
        .is(entity.getProjectIdentifier())
        .and(InputSetEntityKeys.pipelineIdentifier)
        .is(entity.getPipelineIdentifier())
        .and(InputSetEntityKeys.identifier)
        .is(entity.getIdentifier());
    if (entity.getYamlGitConfigRef() != null) {
      criteria.and(InputSetEntityKeys.yamlGitConfigRef)
          .is(entity.getYamlGitConfigRef())
          .and(InputSetEntityKeys.branch)
          .is(entity.getBranch());
    }

    Update update = new Update();
    update.set(InputSetEntityKeys.isInvalid, isInvalid);
    InputSetEntity inputSetEntity = inputSetRepository.switchValidationFlag(criteria, update);
    return inputSetEntity != null;
  }

  @Override
  public boolean markGitSyncedInputSetInvalid(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String identifier, String invalidYaml) {
    Optional<InputSetEntity> optionalInputSetEntity =
        get(accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier, false);
    if (!optionalInputSetEntity.isPresent()) {
      log.warn(String.format(
          "Marking input set [%s] as invalid failed as it does not exist or has been deleted", identifier));
      return false;
    }
    InputSetEntity existingInputSet = optionalInputSetEntity.get();
    InputSetEntity updatedInputSet = existingInputSet.withYaml(invalidYaml)
                                         .withObjectIdOfYaml(EntityObjectIdUtils.getObjectIdOfYaml(invalidYaml))
                                         .withIsEntityInvalid(true);
    makeInputSetUpdateCall(updatedInputSet, ChangeType.NONE);
    return true;
  }

  private InputSetEntity makeInputSetUpdateCall(InputSetEntity entity, ChangeType changeType) {
    try {
      InputSetEntity updatedEntity = inputSetRepository.update(entity, InputSetYamlDTOMapper.toDTO(entity), changeType);

      if (updatedEntity == null) {
        throw new InvalidRequestException(
            format("Input Set [%s], for pipeline [%s], under Project[%s], Organization [%s] could not be updated.",
                entity.getIdentifier(), entity.getPipelineIdentifier(), entity.getProjectIdentifier(),
                entity.getOrgIdentifier()));
      }
      return updatedEntity;
    } catch (Exception e) {
      log.error(String.format("Error while updating input set [%s]", entity.getIdentifier()), e);
      throw new InvalidRequestException(
          String.format("Error while updating input set [%s]: %s", entity.getIdentifier(), e.getMessage()));
    }
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String identifier, Long version) {
    Optional<InputSetEntity> optionalOriginalEntity =
        get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier, false);
    if (!optionalOriginalEntity.isPresent()) {
      throw new InvalidRequestException(
          format("Input Set [%s], for pipeline [%s], under Project[%s], Organization [%s] doesn't exist.", identifier,
              pipelineIdentifier, projectIdentifier, orgIdentifier));
    }
    InputSetEntity existingEntity = optionalOriginalEntity.get();
    if (version != null && !version.equals(existingEntity.getVersion())) {
      throw new InvalidRequestException(format(
          "Input Set [%s], for pipeline [%s], under Project[%s], Organization [%s] is not on the correct version.",
          identifier, pipelineIdentifier, projectIdentifier, orgIdentifier));
    }
    InputSetEntity entityWithDelete = existingEntity.withDeleted(true);
    try {
      InputSetEntity deletedEntity =
          inputSetRepository.delete(entityWithDelete, InputSetYamlDTOMapper.toDTO(entityWithDelete));

      if (deletedEntity.getDeleted()) {
        return true;
      } else {
        throw new InvalidRequestException(
            format("Input Set [%s], for pipeline [%s], under Project[%s], Organization [%s] couldn't be deleted.",
                identifier, pipelineIdentifier, projectIdentifier, orgIdentifier));
      }
    } catch (Exception e) {
      log.error(String.format("Error while deleting input set [%s]", identifier), e);
      throw new InvalidRequestException(
          String.format("Error while deleting input set [%s]: %s", identifier, e.getMessage()));
    }
  }

  @Override
  public Page<InputSetEntity> list(
      Criteria criteria, Pageable pageable, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return inputSetRepository.findAll(criteria, pageable, accountIdentifier, orgIdentifier, projectIdentifier);
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
