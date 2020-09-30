package io.harness.cdng.inputset.services.impl;

import static io.harness.exception.WingsException.USER_SRE;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.client.result.UpdateResult;
import io.harness.cdng.inputset.repository.spring.InputSetRepository;
import io.harness.cdng.inputset.services.InputSetEntityService;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity.BaseInputSetEntityKeys;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InputSetEntityServiceImpl implements InputSetEntityService {
  private final InputSetRepository inputSetRepository;

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "InputSet [%s] under Project[%s], Organization [%s] for Pipeline [%s] already exists";

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public BaseInputSetEntity create(@NotNull @Valid BaseInputSetEntity baseInputSetEntity) {
    try {
      validatePresenceOfRequiredFields(baseInputSetEntity.getAccountId(), baseInputSetEntity.getOrgIdentifier(),
          baseInputSetEntity.getProjectIdentifier(), baseInputSetEntity.getPipelineIdentifier(),
          baseInputSetEntity.getIdentifier());
      setName(baseInputSetEntity);
      return inputSetRepository.save(baseInputSetEntity);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format(DUP_KEY_EXP_FORMAT_STRING, baseInputSetEntity.getIdentifier(),
              baseInputSetEntity.getProjectIdentifier(), baseInputSetEntity.getOrgIdentifier(),
              baseInputSetEntity.getPipelineIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<BaseInputSetEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String inputSetIdentifier, boolean deleted) {
    return inputSetRepository
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndIdentifierAndDeletedNot(
            accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, !deleted);
  }

  @Override
  public BaseInputSetEntity update(BaseInputSetEntity baseInputSetEntity) {
    try {
      validatePresenceOfRequiredFields(baseInputSetEntity.getAccountId(), baseInputSetEntity.getOrgIdentifier(),
          baseInputSetEntity.getProjectIdentifier(), baseInputSetEntity.getPipelineIdentifier(),
          baseInputSetEntity.getIdentifier());
      Criteria criteria = getInputSetEqualityCriteria(baseInputSetEntity, baseInputSetEntity.getDeleted());
      UpdateResult updateResult = inputSetRepository.update(criteria, baseInputSetEntity);
      if (!updateResult.wasAcknowledged() || updateResult.getModifiedCount() != 1) {
        throw new InvalidRequestException(String.format(
            "InputSet [%s] under Project[%s], Organization [%s] for Pipeline [%s] couldn't be updated or doesn't exist.",
            baseInputSetEntity.getIdentifier(), baseInputSetEntity.getProjectIdentifier(),
            baseInputSetEntity.getOrgIdentifier(), baseInputSetEntity.getPipelineIdentifier()));
      }
      return baseInputSetEntity;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format(DUP_KEY_EXP_FORMAT_STRING, baseInputSetEntity.getIdentifier(),
              baseInputSetEntity.getProjectIdentifier(), baseInputSetEntity.getOrgIdentifier(),
              baseInputSetEntity.getPipelineIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Page<BaseInputSetEntity> list(Criteria criteria, Pageable pageable) {
    return inputSetRepository.findAll(criteria, pageable);
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String inputSetIdentifier) {
    Criteria criteria = getInputSetEqualityCriteria(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, false);
    UpdateResult updateResult = inputSetRepository.delete(criteria);
    if (!updateResult.wasAcknowledged()) {
      throw new InvalidRequestException(
          String.format("InputSet [%s] under Project[%s], Organization [%s] for Pipeline [%s] couldn't be deleted.",
              inputSetIdentifier, projectIdentifier, orgIdentifier, pipelineIdentifier));
    }
    return true;
  }

  @Override
  public List<BaseInputSetEntity> getGivenInputSetList(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, Set<String> inputSetIdentifiersList) {
    return inputSetRepository
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndDeletedNotAndIdentifierIn(
            accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, true, inputSetIdentifiersList);
  }

  private void setName(BaseInputSetEntity baseInputSetEntity) {
    if (EmptyPredicate.isEmpty(baseInputSetEntity.getName())) {
      baseInputSetEntity.setName(baseInputSetEntity.getIdentifier());
    }
  }

  private Criteria getInputSetEqualityCriteria(@Valid BaseInputSetEntity requestCDInputSet, boolean deleted) {
    return getInputSetEqualityCriteria(requestCDInputSet.getAccountId(), requestCDInputSet.getOrgIdentifier(),
        requestCDInputSet.getProjectIdentifier(), requestCDInputSet.getPipelineIdentifier(),
        requestCDInputSet.getIdentifier(), deleted);
  }

  private Criteria getInputSetEqualityCriteria(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String inputSetIdentifier, boolean deleted) {
    return Criteria.where(BaseInputSetEntityKeys.accountId)
        .is(accountId)
        .and(BaseInputSetEntityKeys.orgIdentifier)
        .is(orgIdentifier)
        .and(BaseInputSetEntityKeys.projectIdentifier)
        .is(projectIdentifier)
        .and(BaseInputSetEntityKeys.pipelineIdentifier)
        .is(pipelineIdentifier)
        .and(BaseInputSetEntityKeys.identifier)
        .is(inputSetIdentifier)
        .and(BaseInputSetEntityKeys.deleted)
        .is(deleted);
  }
}
