package io.harness.ngpipeline.inputset.services.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import io.harness.beans.InputSetReference;
import io.harness.data.structure.EmptyPredicate;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ngpipeline.inputset.repository.spring.InputSetRepository;
import io.harness.ngpipeline.inputset.services.InputSetEntityService;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity.BaseInputSetEntityKeys;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InputSetEntityServiceImpl implements InputSetEntityService {
  private final InputSetRepository inputSetRepository;
  private final EntitySetupUsageClient entitySetupUsageClient;

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
    validatePresenceOfRequiredFields(baseInputSetEntity.getAccountId(), baseInputSetEntity.getOrgIdentifier(),
        baseInputSetEntity.getProjectIdentifier(), baseInputSetEntity.getPipelineIdentifier(),
        baseInputSetEntity.getIdentifier());
    Criteria criteria = getInputSetEqualityCriteria(baseInputSetEntity, baseInputSetEntity.getDeleted());
    BaseInputSetEntity updateResult = inputSetRepository.update(criteria, baseInputSetEntity);
    if (updateResult == null) {
      throw new InvalidRequestException(
          String.format("Input Set [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
              baseInputSetEntity.getIdentifier(), baseInputSetEntity.getProjectIdentifier(),
              baseInputSetEntity.getOrgIdentifier()));
    }
    return updateResult;
  }

  @Override
  public Page<BaseInputSetEntity> list(Criteria criteria, Pageable pageable) {
    return inputSetRepository.findAll(criteria, pageable);
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String inputSetIdentifier, Long version) {
    checkThatTheInputSetIsNotUsedByOthers(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier);
    Criteria criteria = getInputSetEqualityCriteria(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, false, version);
    UpdateResult updateResult = inputSetRepository.delete(criteria);
    if (!updateResult.wasAcknowledged() || updateResult.getModifiedCount() != 1) {
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

  @Override
  public void deleteInputSetsOfPipeline(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    Criteria criteria = Criteria.where(BaseInputSetEntityKeys.accountId)
                            .is(accountId)
                            .and(BaseInputSetEntityKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(BaseInputSetEntityKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(BaseInputSetEntityKeys.pipelineIdentifier)
                            .is(pipelineIdentifier);
    UpdateResult updateResult = inputSetRepository.delete(criteria);
    if (!updateResult.wasAcknowledged()) {
      throw new InvalidRequestException(
          String.format("InputSets under Project[%s], Organization [%s] for Pipeline [%s] couldn't be deleted.",
              projectIdentifier, orgIdentifier, pipelineIdentifier));
    }
  }

  @VisibleForTesting
  void checkThatTheInputSetIsNotUsedByOthers(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String inputSetIdentifier) {
    List<EntityDetail> referredByEntities;
    InputSetReference inputSetReference = InputSetReference.builder()
                                              .accountIdentifier(accountId)
                                              .orgIdentifier(orgIdentifier)
                                              .projectIdentifier(projectIdentifier)
                                              .pipelineIdentifier(pipelineIdentifier)
                                              .identifier(inputSetIdentifier)
                                              .build();
    try {
      Page<EntitySetupUsageDTO> entitySetupUsageDTOS = execute(
          entitySetupUsageClient.listAllEntityUsage(0, 10, accountId, inputSetReference.getFullyQualifiedName(), ""));
      referredByEntities = entitySetupUsageDTOS.stream()
                               .map(EntitySetupUsageDTO::getReferredByEntity)
                               .collect(Collectors.toCollection(LinkedList::new));
    } catch (Exception ex) {
      logger.info("Encountered exception while requesting the Entity Reference records of [{}], with exception",
          inputSetIdentifier, ex);
      throw new UnexpectedException(
          "Error while deleting the input set as was not able to check entity reference records.");
    }
    if (EmptyPredicate.isNotEmpty(referredByEntities)) {
      throw new InvalidRequestException(String.format(
          "Could not delete the Input Set %s as it is referenced by other entities - " + referredByEntities.toString(),
          inputSetIdentifier));
    }
  }

  private void setName(BaseInputSetEntity baseInputSetEntity) {
    if (EmptyPredicate.isEmpty(baseInputSetEntity.getName())) {
      baseInputSetEntity.setName(baseInputSetEntity.getIdentifier());
    }
  }

  private Criteria getInputSetEqualityCriteria(@Valid BaseInputSetEntity requestInputSet, boolean deleted) {
    return getInputSetEqualityCriteria(requestInputSet.getAccountId(), requestInputSet.getOrgIdentifier(),
        requestInputSet.getProjectIdentifier(), requestInputSet.getPipelineIdentifier(),
        requestInputSet.getIdentifier(), deleted, requestInputSet.getVersion());
  }

  private Criteria getInputSetEqualityCriteria(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String inputSetIdentifier, boolean deleted, Long version) {
    Criteria criteria = Criteria.where(BaseInputSetEntityKeys.accountId)
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

    if (version != null) {
      criteria.and(BaseInputSetEntityKeys.version).is(version);
    }
    return criteria;
  }
}
