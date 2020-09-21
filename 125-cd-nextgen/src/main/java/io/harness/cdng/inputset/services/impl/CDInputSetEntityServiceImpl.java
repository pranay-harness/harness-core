package io.harness.cdng.inputset.services.impl;

import static io.harness.exception.WingsException.USER_SRE;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.client.result.UpdateResult;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity.CDInputSetEntityKeys;
import io.harness.cdng.inputset.repository.spring.CDInputSetRepository;
import io.harness.cdng.inputset.services.CDInputSetEntityService;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class CDInputSetEntityServiceImpl implements CDInputSetEntityService {
  private final CDInputSetRepository cdInputSetRepository;
  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "InputSet [%s] under Project[%s], Organization [%s] for Pipeline [%s] already exists";

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public CDInputSetEntity create(@NotNull @Valid CDInputSetEntity cdInputSetEntity) {
    try {
      validatePresenceOfRequiredFields(cdInputSetEntity.getAccountId(), cdInputSetEntity.getOrgIdentifier(),
          cdInputSetEntity.getProjectIdentifier(), cdInputSetEntity.getPipelineIdentifier(),
          cdInputSetEntity.getIdentifier());
      setName(cdInputSetEntity);
      return cdInputSetRepository.save(cdInputSetEntity);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format(DUP_KEY_EXP_FORMAT_STRING, cdInputSetEntity.getIdentifier(),
              cdInputSetEntity.getProjectIdentifier(), cdInputSetEntity.getOrgIdentifier(),
              cdInputSetEntity.getPipelineIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<CDInputSetEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String inputSetIdentifier, boolean deleted) {
    return cdInputSetRepository
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndIdentifierAndDeletedNot(
            accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, !deleted);
  }

  @Override
  public CDInputSetEntity update(CDInputSetEntity requestCDInputSet) {
    try {
      validatePresenceOfRequiredFields(requestCDInputSet.getAccountId(), requestCDInputSet.getOrgIdentifier(),
          requestCDInputSet.getProjectIdentifier(), requestCDInputSet.getPipelineIdentifier(),
          requestCDInputSet.getIdentifier());
      Criteria criteria = getInputSetEqualityCriteria(requestCDInputSet, requestCDInputSet.getDeleted());
      UpdateResult updateResult = cdInputSetRepository.update(criteria, requestCDInputSet);
      if (!updateResult.wasAcknowledged() || updateResult.getModifiedCount() != 1) {
        throw new InvalidRequestException(String.format(
            "InputSet [%s] under Project[%s], Organization [%s] for Pipeline [%s] couldn't be updated or doesn't exist.",
            requestCDInputSet.getIdentifier(), requestCDInputSet.getProjectIdentifier(),
            requestCDInputSet.getOrgIdentifier(), requestCDInputSet.getPipelineIdentifier()));
      }
      return requestCDInputSet;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format(DUP_KEY_EXP_FORMAT_STRING, requestCDInputSet.getIdentifier(),
              requestCDInputSet.getProjectIdentifier(), requestCDInputSet.getOrgIdentifier(),
              requestCDInputSet.getPipelineIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public CDInputSetEntity upsert(CDInputSetEntity requestCDInputSet) {
    try {
      validatePresenceOfRequiredFields(requestCDInputSet.getAccountId(), requestCDInputSet.getOrgIdentifier(),
          requestCDInputSet.getProjectIdentifier(), requestCDInputSet.getPipelineIdentifier(),
          requestCDInputSet.getIdentifier());
      Criteria criteria = getInputSetEqualityCriteria(requestCDInputSet, requestCDInputSet.getDeleted());
      UpdateResult upsertResult = cdInputSetRepository.upsert(criteria, requestCDInputSet);
      if (!upsertResult.wasAcknowledged()) {
        throw new InvalidRequestException(String.format(
            "InputSet [%s] under Project[%s], Organization [%s] for Pipeline [%s] couldn't be upserted or doesn't exist.",
            requestCDInputSet.getIdentifier(), requestCDInputSet.getProjectIdentifier(),
            requestCDInputSet.getOrgIdentifier(), requestCDInputSet.getPipelineIdentifier()));
      }
      return requestCDInputSet;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format(DUP_KEY_EXP_FORMAT_STRING, requestCDInputSet.getIdentifier(),
              requestCDInputSet.getProjectIdentifier(), requestCDInputSet.getOrgIdentifier(),
              requestCDInputSet.getPipelineIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Page<CDInputSetEntity> list(Criteria criteria, Pageable pageable) {
    return cdInputSetRepository.findAll(criteria, pageable);
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String inputSetIdentifier) {
    CDInputSetEntity cdInputSetEntity = CDInputSetEntity.builder()
                                            .accountId(accountId)
                                            .orgIdentifier(orgIdentifier)
                                            .projectIdentifier(projectIdentifier)
                                            .pipelineIdentifier(pipelineIdentifier)
                                            .identifier(inputSetIdentifier)
                                            .build();
    Criteria criteria = getInputSetEqualityCriteria(cdInputSetEntity, false);
    UpdateResult updateResult = cdInputSetRepository.delete(criteria);
    if (!updateResult.wasAcknowledged()) {
      throw new InvalidRequestException(
          String.format("InputSet [%s] under Project[%s], Organization [%s] for Pipeline [%s] couldn't be deleted.",
              inputSetIdentifier, projectIdentifier, orgIdentifier, pipelineIdentifier));
    }
    return true;
  }

  private void setName(CDInputSetEntity cdInputSetEntity) {
    if (EmptyPredicate.isEmpty(cdInputSetEntity.getName())) {
      cdInputSetEntity.setName(cdInputSetEntity.getIdentifier());
    }
  }

  private Criteria getInputSetEqualityCriteria(@Valid CDInputSetEntity requestCDInputSet, boolean deleted) {
    return Criteria.where(CDInputSetEntityKeys.accountId)
        .is(requestCDInputSet.getAccountId())
        .and(CDInputSetEntityKeys.orgIdentifier)
        .is(requestCDInputSet.getOrgIdentifier())
        .and(CDInputSetEntityKeys.projectIdentifier)
        .is(requestCDInputSet.getProjectIdentifier())
        .and(CDInputSetEntityKeys.pipelineIdentifier)
        .is(requestCDInputSet.getPipelineIdentifier())
        .and(CDInputSetEntityKeys.identifier)
        .is(requestCDInputSet.getIdentifier())
        .and(CDInputSetEntityKeys.deleted)
        .is(deleted);
  }
}
