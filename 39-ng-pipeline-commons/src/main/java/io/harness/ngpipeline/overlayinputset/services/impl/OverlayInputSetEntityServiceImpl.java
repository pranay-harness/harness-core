package io.harness.ngpipeline.overlayinputset.services.impl;

import static io.harness.exception.WingsException.USER_SRE;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.client.result.UpdateResult;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity.OverlayInputSetEntityKeys;
import io.harness.ngpipeline.overlayinputset.repository.spring.OverlayInputSetRepository;
import io.harness.ngpipeline.overlayinputset.services.OverlayInputSetEntityService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class OverlayInputSetEntityServiceImpl implements OverlayInputSetEntityService {
  private final OverlayInputSetRepository overlayInputSetRepository;
  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "InputSet [%s] under Project[%s], Organization [%s] for Pipeline [%s] already exists";

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public OverlayInputSetEntity create(OverlayInputSetEntity overlayInputSetEntity) {
    try {
      validatePresenceOfRequiredFields(overlayInputSetEntity.getAccountId(), overlayInputSetEntity.getOrgIdentifier(),
          overlayInputSetEntity.getProjectIdentifier(), overlayInputSetEntity.getPipelineIdentifier(),
          overlayInputSetEntity.getIdentifier());
      setName(overlayInputSetEntity);
      return overlayInputSetRepository.save(overlayInputSetEntity);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format(DUP_KEY_EXP_FORMAT_STRING, overlayInputSetEntity.getIdentifier(),
              overlayInputSetEntity.getProjectIdentifier(), overlayInputSetEntity.getOrgIdentifier(),
              overlayInputSetEntity.getPipelineIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<OverlayInputSetEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String inputSetIdentifier, boolean deleted) {
    return overlayInputSetRepository
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndIdentifierAndDeletedNot(
            accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, !deleted);
  }

  @Override
  public OverlayInputSetEntity update(OverlayInputSetEntity requestOverlayInputSet) {
    try {
      validatePresenceOfRequiredFields(requestOverlayInputSet.getAccountId(), requestOverlayInputSet.getOrgIdentifier(),
          requestOverlayInputSet.getProjectIdentifier(), requestOverlayInputSet.getPipelineIdentifier(),
          requestOverlayInputSet.getIdentifier());
      Criteria criteria = getInputSetEqualityCriteria(requestOverlayInputSet, requestOverlayInputSet.getDeleted());
      UpdateResult updateResult = overlayInputSetRepository.update(criteria, requestOverlayInputSet);
      if (!updateResult.wasAcknowledged() || updateResult.getModifiedCount() != 1) {
        throw new InvalidRequestException(String.format(
            "InputSet [%s] under Project[%s], Organization [%s] for Pipeline [%s] couldn't be updated or doesn't exist.",
            requestOverlayInputSet.getIdentifier(), requestOverlayInputSet.getProjectIdentifier(),
            requestOverlayInputSet.getOrgIdentifier(), requestOverlayInputSet.getPipelineIdentifier()));
      }
      return requestOverlayInputSet;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format(DUP_KEY_EXP_FORMAT_STRING, requestOverlayInputSet.getIdentifier(),
              requestOverlayInputSet.getProjectIdentifier(), requestOverlayInputSet.getOrgIdentifier(),
              requestOverlayInputSet.getPipelineIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public OverlayInputSetEntity upsert(OverlayInputSetEntity requestOverlayInputSet) {
    try {
      validatePresenceOfRequiredFields(requestOverlayInputSet.getAccountId(), requestOverlayInputSet.getOrgIdentifier(),
          requestOverlayInputSet.getProjectIdentifier(), requestOverlayInputSet.getPipelineIdentifier(),
          requestOverlayInputSet.getIdentifier());
      Criteria criteria = getInputSetEqualityCriteria(requestOverlayInputSet, requestOverlayInputSet.getDeleted());
      UpdateResult updateResult = overlayInputSetRepository.upsert(criteria, requestOverlayInputSet);
      if (!updateResult.wasAcknowledged()) {
        throw new InvalidRequestException(String.format(
            "InputSet [%s] under Project[%s], Organization [%s] for Pipeline [%s] couldn't be updated or doesn't exist.",
            requestOverlayInputSet.getIdentifier(), requestOverlayInputSet.getProjectIdentifier(),
            requestOverlayInputSet.getOrgIdentifier(), requestOverlayInputSet.getPipelineIdentifier()));
      }
      return requestOverlayInputSet;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format(DUP_KEY_EXP_FORMAT_STRING, requestOverlayInputSet.getIdentifier(),
              requestOverlayInputSet.getProjectIdentifier(), requestOverlayInputSet.getOrgIdentifier(),
              requestOverlayInputSet.getPipelineIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Page<OverlayInputSetEntity> list(Criteria criteria, Pageable pageable) {
    return overlayInputSetRepository.findAll(criteria, pageable);
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String inputSetIdentifier) {
    OverlayInputSetEntity overlayInputSetEntity = OverlayInputSetEntity.builder()
                                                      .accountId(accountId)
                                                      .orgIdentifier(orgIdentifier)
                                                      .projectIdentifier(projectIdentifier)
                                                      .pipelineIdentifier(pipelineIdentifier)
                                                      .identifier(inputSetIdentifier)
                                                      .build();
    Criteria criteria = getInputSetEqualityCriteria(overlayInputSetEntity, false);
    UpdateResult updateResult = overlayInputSetRepository.delete(criteria);
    if (!updateResult.wasAcknowledged()) {
      throw new InvalidRequestException(
          String.format("InputSet [%s] under Project[%s], Organization [%s] for Pipeline [%s] couldn't be deleted.",
              inputSetIdentifier, projectIdentifier, orgIdentifier, pipelineIdentifier));
    }
    return true;
  }

  private void setName(OverlayInputSetEntity cdInputSetEntity) {
    if (EmptyPredicate.isEmpty(cdInputSetEntity.getName())) {
      cdInputSetEntity.setName(cdInputSetEntity.getIdentifier());
    }
  }

  private Criteria getInputSetEqualityCriteria(@Valid OverlayInputSetEntity requestOverlayInputSet, boolean deleted) {
    return Criteria.where(OverlayInputSetEntityKeys.accountId)
        .is(requestOverlayInputSet.getAccountId())
        .and(OverlayInputSetEntityKeys.orgIdentifier)
        .is(requestOverlayInputSet.getOrgIdentifier())
        .and(OverlayInputSetEntityKeys.projectIdentifier)
        .is(requestOverlayInputSet.getProjectIdentifier())
        .and(OverlayInputSetEntityKeys.pipelineIdentifier)
        .is(requestOverlayInputSet.getPipelineIdentifier())
        .and(OverlayInputSetEntityKeys.identifier)
        .is(requestOverlayInputSet.getIdentifier())
        .and(OverlayInputSetEntityKeys.deleted)
        .is(deleted);
  }
}
