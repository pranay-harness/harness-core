package io.harness.ngpipeline.inputset.services.impl;

import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.EntityType;
import io.harness.annotations.dev.ToBeDeleted;
import io.harness.beans.InputSetReference;
import io.harness.data.structure.EmptyPredicate;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ngpipeline.inputset.beans.entities.InputSetEntity;
import io.harness.ngpipeline.inputset.services.InputSetEntityService;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity.BaseInputSetEntityKeys;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.repositories.inputset.spring.InputSetRepository;
import io.harness.walktree.visitor.SimpleVisitorFactory;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractorVisitor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@ToBeDeleted
@Deprecated
public class InputSetEntityServiceImpl implements InputSetEntityService {
  private final InputSetRepository inputSetRepository;
  private final EntitySetupUsageClient entitySetupUsageClient;
  private final SimpleVisitorFactory simpleVisitorFactory;

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "InputSet [%s] under Project[%s], Organization [%s] for Pipeline [%s] already exists";

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public BaseInputSetEntity create(@NotNull @Valid BaseInputSetEntity baseInputSetEntity) {
    validatePresenceOfRequiredFields(baseInputSetEntity.getAccountId(), baseInputSetEntity.getOrgIdentifier(),
        baseInputSetEntity.getProjectIdentifier(), baseInputSetEntity.getPipelineIdentifier(),
        baseInputSetEntity.getIdentifier());
    setName(baseInputSetEntity);
    if (baseInputSetEntity instanceof InputSetEntity) {
      return saveInputSetAndReferences((InputSetEntity) baseInputSetEntity);
    } else {
      return saveInputSet(baseInputSetEntity);
    }
  }

  private BaseInputSetEntity saveInputSetAndReferences(InputSetEntity inputSetEntity) {
    inputSetEntity.setReferredEntities(getReferences(inputSetEntity.getAccountId(), inputSetEntity.getOrgIdentifier(),
        inputSetEntity.getProjectIdentifier(), inputSetEntity.getInputSetConfig().getPipeline()));
    BaseInputSetEntity savedEntity = saveInputSet(inputSetEntity);
    saveReferencesInInputSet(inputSetEntity);
    return savedEntity;
  }

  private BaseInputSetEntity saveInputSet(BaseInputSetEntity inputSetEntity) {
    try {
      return inputSetRepository.save(inputSetEntity);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(String.format(DUP_KEY_EXP_FORMAT_STRING, inputSetEntity.getIdentifier(),
                                            inputSetEntity.getProjectIdentifier(), inputSetEntity.getOrgIdentifier(),
                                            inputSetEntity.getPipelineIdentifier()),
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
    if (baseInputSetEntity instanceof InputSetEntity) {
      return updateInputSetAndReferences((InputSetEntity) baseInputSetEntity);
    } else {
      return updateInputSet(baseInputSetEntity);
    }
  }

  private BaseInputSetEntity updateInputSetAndReferences(InputSetEntity inputSetEntity) {
    inputSetEntity.setReferredEntities(getReferences(inputSetEntity.getAccountId(), inputSetEntity.getOrgIdentifier(),
        inputSetEntity.getProjectIdentifier(), inputSetEntity.getInputSetConfig().getPipeline()));

    Set<EntityDetail> oldEntities;
    try {
      InputSetEntity oldVersion = (InputSetEntity) get(inputSetEntity.getAccountId(), inputSetEntity.getOrgIdentifier(),
          inputSetEntity.getProjectIdentifier(), inputSetEntity.getPipelineIdentifier(), inputSetEntity.getIdentifier(),
          false)
                                      .get();
      oldEntities = oldVersion.getReferredEntities();
    } catch (Exception e) {
      throw new InvalidRequestException(String.format(
          "Input Set [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
          inputSetEntity.getIdentifier(), inputSetEntity.getProjectIdentifier(), inputSetEntity.getOrgIdentifier()));
    }

    BaseInputSetEntity updateResult = updateInputSet(inputSetEntity);
    updateReferencesPresentInInputSet(inputSetEntity, oldEntities);
    return updateResult;
  }

  private BaseInputSetEntity updateInputSet(BaseInputSetEntity inputSetEntity) {
    Criteria criteria = getInputSetEqualityCriteria(inputSetEntity, inputSetEntity.getDeleted());
    BaseInputSetEntity updateResult = inputSetRepository.update(criteria, inputSetEntity);
    if (updateResult == null) {
      throw new InvalidRequestException(String.format(
          "Input Set [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
          inputSetEntity.getIdentifier(), inputSetEntity.getProjectIdentifier(), inputSetEntity.getOrgIdentifier()));
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
      Page<EntitySetupUsageDTO> entitySetupUsageDTOS = getResponse(entitySetupUsageClient.listAllEntityUsage(
          0, 10, accountId, inputSetReference.getFullyQualifiedName(), EntityType.INPUT_SETS, ""));
      referredByEntities = entitySetupUsageDTOS.stream()
                               .map(EntitySetupUsageDTO::getReferredByEntity)
                               .collect(Collectors.toCollection(LinkedList::new));
    } catch (Exception ex) {
      log.info("Encountered exception while requesting the Entity Reference records of [{}], with exception",
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

  private Set<EntityDetail> getReferences(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, NgPipeline pipeline) {
    EntityReferenceExtractorVisitor visitor = simpleVisitorFactory.obtainEntityReferenceExtractorVisitor(
        accountIdentifier, orgIdentifier, projectIdentifier, null);
    visitor.walkElementTree(pipeline);
    return new HashSet<>(); // visitor.getEntityReferenceSet();
  }

  private EntityDetail getInputSetEntityDetail(InputSetEntity inputSetEntity) {
    InputSetReference inputSetReference = InputSetReference.builder()
                                              .accountIdentifier(inputSetEntity.getAccountId())
                                              .orgIdentifier(inputSetEntity.getOrgIdentifier())
                                              .projectIdentifier(inputSetEntity.getProjectIdentifier())
                                              .pipelineIdentifier(inputSetEntity.getPipelineIdentifier())
                                              .identifier(inputSetEntity.getIdentifier())
                                              .build();
    return EntityDetail.builder().entityRef(inputSetReference).type(EntityType.INPUT_SETS).build();
  }

  private void saveReferencesInInputSet(InputSetEntity inputSetEntity) {
    Set<EntityDetail> referredEntities = inputSetEntity.getReferredEntities();

    EntityDetail referredByEntity = getInputSetEntityDetail(inputSetEntity);
    for (EntityDetail entity : referredEntities) {
      EntitySetupUsageDTO entitySetupUsageDTO = EntitySetupUsageDTO.builder()
                                                    .accountIdentifier(inputSetEntity.getAccountId())
                                                    .referredEntity(entity)
                                                    .referredByEntity(referredByEntity)
                                                    .build();
      getResponse(entitySetupUsageClient.save(entitySetupUsageDTO));
    }
  }

  private void updateReferencesPresentInInputSet(InputSetEntity inputSetEntity, Set<EntityDetail> oldEntities) {
    EntityDetail referredByEntity = getInputSetEntityDetail(inputSetEntity);

    Set<EntityDetail> newEntities = inputSetEntity.getReferredEntities();

    Set<EntityDetail> commonEntities = new HashSet<>(newEntities);
    commonEntities.removeIf(entity -> !oldEntities.contains(entity));

    Set<EntityDetail> entitiesToRemove = new HashSet<>(oldEntities);
    entitiesToRemove.removeIf(commonEntities::contains);

    Set<EntityDetail> entitiesToAdd = new HashSet<>(newEntities);
    entitiesToAdd.removeIf(commonEntities::contains);

    // adds entities present in the update but not in the old version
    for (EntityDetail entity : entitiesToAdd) {
      EntitySetupUsageDTO entitySetupUsageDTO = EntitySetupUsageDTO.builder()
                                                    .accountIdentifier(inputSetEntity.getAccountId())
                                                    .referredEntity(entity)
                                                    .referredByEntity(referredByEntity)
                                                    .build();
      getResponse(entitySetupUsageClient.save(entitySetupUsageDTO));
    }

    // removes entities present in the old version but not in the update
    for (EntityDetail entity : entitiesToRemove) {
      getResponse(
          entitySetupUsageClient.delete(inputSetEntity.getAccountId(), entity.getEntityRef().getFullyQualifiedName(),
              entity.getType(), referredByEntity.getEntityRef().getFullyQualifiedName(), referredByEntity.getType()));
    }
  }
}
