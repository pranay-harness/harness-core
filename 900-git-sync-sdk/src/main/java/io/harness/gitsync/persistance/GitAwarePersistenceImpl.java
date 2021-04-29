package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.gitsync.interceptor.GitSyncConstants.DEFAULT_BRANCH;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.exception.UnexpectedException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.branching.EntityGitBranchMetadata;
import io.harness.gitsync.branching.EntityGitBranchMetadata.EntityGitBranchMetadataKeys;
import io.harness.gitsync.branching.GitBranchingHelper;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchThreadLocal;
import io.harness.gitsync.scm.EntityToYamlStringUtils;
import io.harness.gitsync.scm.SCMGitSyncHelper;
import io.harness.gitsync.scm.beans.ScmPushResponse;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.utils.NGYamlUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.StringValue;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@OwnedBy(DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class GitAwarePersistenceImpl implements GitAwarePersistence {
  private MongoTemplate mongoTemplate;
  private EntityKeySource entityKeySource;
  private GitBranchingHelper gitBranchingHelper;
  private Map<String, GitSdkEntityHandlerInterface> gitPersistenceHelperServiceMap;
  private SCMGitSyncHelper scmGitSyncHelper;
  private GitSyncMsvcHelper gitSyncMsvcHelper;

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> Long count(@NotNull Criteria criteria,
      String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    final Criteria gitSyncCriteria =
        updateCriteriaIfGitSyncEnabled(projectIdentifier, orgIdentifier, accountId, entityClass);
    List<Criteria> criteriaList = Arrays.asList(criteria, gitSyncCriteria);
    Query query = new Query()
                      .addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])))
                      .limit(-1)
                      .skip(-1);

    // todo(abhinav): do we have to do anything extra if git sync is not there?
    return mongoTemplate.count(query, entityClass);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> Optional<B> findOne(@NotNull Criteria criteria,
      String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    final Criteria gitSyncCriteria =
        updateCriteriaIfGitSyncEnabled(projectIdentifier, orgIdentifier, accountId, entityClass);
    // todo(abhinav): do we have to do anything extra if git sync is not there?
    List<Criteria> criteriaList = Arrays.asList(criteria, gitSyncCriteria);
    Query query =
        new Query().addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
    final B object = mongoTemplate.findOne(query, entityClass);
    setBranchInObject(object);
    return Optional.ofNullable(object);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> List<B> find(@NotNull Criteria criteria, Pageable pageable,
      String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    final Criteria gitSyncCriteria =
        updateCriteriaIfGitSyncEnabled(projectIdentifier, orgIdentifier, accountId, entityClass);
    // todo(abhinav): do we have to do anything extra if git sync is not there?
    List<Criteria> criteriaList = Arrays.asList(criteria, gitSyncCriteria);
    Query query = new Query()
                      .addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])))
                      .with(pageable);
    final List<B> obj = mongoTemplate.find(query, entityClass);
    return obj.stream().map(this::setBranchInObject).collect(Collectors.toList());
  }

  // Cannot update without a object. Hence removed.
  private <B extends GitSyncableEntity, Y extends YamlDTO> B update(Query query, Update update, ChangeType changeType,
      String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    // todo(abhinav): do we have to do anything extra if git sync is not there?
    final B objectToUpdate = mongoTemplate.findOne(query, entityClass);
    if (objectToUpdate == null) {
      return null;
    }

    final GitEntityInfo gitBranchInfo = GitSyncBranchThreadLocal.get();
    final GitSdkEntityHandlerInterface gitSdkEntityHandlerInterface =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName());
    final EntityDetail entityDetail = gitSdkEntityHandlerInterface.getEntityDetail(objectToUpdate);

    if (changeType != ChangeType.NONE && isGitSyncEnabled(projectIdentifier, orgIdentifier, accountId)) {
      final String yamlString = EntityToYamlStringUtils.getYamlString(objectToUpdate, gitSdkEntityHandlerInterface);
      final ScmPushResponse scmPushResponse =
          scmGitSyncHelper.pushToGit(gitBranchInfo, yamlString, changeType, entityDetail);
      final String objectIdOfYaml = scmPushResponse.getObjectId();
      final EntityGitBranchMetadata entityGitBranchMetadata =
          getEntityGitBranchMetadata(entityDetail, scmPushResponse, objectIdOfYaml);
      update.set(gitSdkEntityHandlerInterface.getObjectIdOfYamlKey(), objectIdOfYaml);
      update.set(gitSdkEntityHandlerInterface.getIsFromDefaultBranchKey(), scmPushResponse.isPushToDefaultBranch());
      update.set(gitSdkEntityHandlerInterface.getYamlGitConfigRefKey(), scmPushResponse.getYamlGitConfigId());
      final B modifiedObject =
          mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), entityClass);
      processGitBranchMetadata(
          modifiedObject, changeType, entityDetail, scmPushResponse, objectIdOfYaml, entityGitBranchMetadata, false);
      gitSyncMsvcHelper.postPushInformationToGitMsvc(entityDetail, scmPushResponse, gitBranchInfo);
    } else {
      update.set(gitSdkEntityHandlerInterface.getIsFromDefaultBranchKey(), true);
    }
    return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), entityClass);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> boolean exists(@NotNull Criteria criteria,
      String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    final Criteria gitSyncCriteria =
        updateCriteriaIfGitSyncEnabled(projectIdentifier, orgIdentifier, accountId, entityClass);
    List<Criteria> criteriaList = Arrays.asList(criteria, gitSyncCriteria);
    Query query =
        new Query().addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
    // todo(abhinav): do we have to do anything extra if git sync is not there?
    return mongoTemplate.exists(query, entityClass);
  }

  private Criteria updateCriteriaIfGitSyncEnabled(
      String projectIdentifier, String orgIdentifier, String accountId, Class entityClass) {
    if (isGitSyncEnabled(projectIdentifier, orgIdentifier, accountId)) {
      final GitSdkEntityHandlerInterface gitSdkEntityHandlerInterface =
          gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName());
      final GitEntityInfo gitBranchInfo = GitSyncBranchThreadLocal.get();
      final List<String> objectId;
      if (gitBranchInfo == null || gitBranchInfo.getYamlGitConfigId() == null || gitBranchInfo.getBranch() == null
          || gitBranchInfo.getYamlGitConfigId().equals(DEFAULT_BRANCH)
          || gitBranchInfo.getBranch().equals(DEFAULT_BRANCH)) {
        return new Criteria().andOperator(
            new Criteria().orOperator(Criteria.where(gitSdkEntityHandlerInterface.getIsFromDefaultBranchKey()).is(true),
                Criteria.where(gitSdkEntityHandlerInterface.getIsFromDefaultBranchKey()).exists(false)));
      } else {
        objectId = gitBranchingHelper.getObjectIdForYamlGitConfigBranchAndScope(gitBranchInfo.getYamlGitConfigId(),
            gitBranchInfo.getBranch(), projectIdentifier, orgIdentifier, accountId, getEntityType(entityClass));
        return new Criteria().and(gitSdkEntityHandlerInterface.getObjectIdOfYamlKey()).in(objectId);
      }
    }
    return new Criteria();
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> B save(
      B objectToSave, ChangeType changeType, Class<B> entityClass) {
    final Supplier<Y> yamlFromEntity =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName()).getYamlFromEntity(objectToSave);
    return save(objectToSave, yamlFromEntity.get(), changeType, entityClass);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> B save(
      B objectToSave, Y yaml, ChangeType changeType, Class<B> entityClass) {
    final GitEntityInfo gitBranchInfo = GitSyncBranchThreadLocal.get();
    final GitSdkEntityHandlerInterface gitSdkEntityHandlerInterface =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName());
    final EntityDetail entityDetail = gitSdkEntityHandlerInterface.getEntityDetail(objectToSave);
    B savedObject;
    if (changeType != ChangeType.NONE
        && isGitSyncEnabled(entityDetail.getEntityRef().getProjectIdentifier(),
            entityDetail.getEntityRef().getOrgIdentifier(), entityDetail.getEntityRef().getAccountIdentifier())) {
      final String yamlString = NGYamlUtils.getYamlString(yaml);

      final ScmPushResponse scmPushResponse =
          scmGitSyncHelper.pushToGit(gitBranchInfo, yamlString, changeType, entityDetail);

      final String objectIdOfYaml = scmPushResponse.getObjectId();
      objectToSave.setObjectIdOfYaml(objectIdOfYaml);
      objectToSave.setYamlGitConfigRef(scmPushResponse.getYamlGitConfigId());
      objectToSave.setIsFromDefaultBranch(scmPushResponse.isPushToDefaultBranch());

      final EntityGitBranchMetadata entityGitBranchMetadata =
          getEntityGitBranchMetadata(entityDetail, scmPushResponse, objectIdOfYaml);
      // Case 1: if objectid is already present dont save and update branch and is default.
      // Case 2: if object if is not present save new .

      boolean newObjectSaved = false;
      if (entityGitBranchMetadata == null) {
        savedObject = mongoTemplate.save(objectToSave);
        newObjectSaved = true;
      } else {
        final String uuidOfEntity = entityGitBranchMetadata.getUuidOfEntity();
        Criteria criteria = Criteria.where(gitSdkEntityHandlerInterface.getUuidKey()).is(uuidOfEntity);
        final B alreadySavedObject = mongoTemplate.findOne(query(criteria), entityClass);
        if (alreadySavedObject == null) {
          log.error(
              "Saved object deleted hence saving again. uuid: [{}], entityclass: [{}]", uuidOfEntity, entityClass);
          savedObject = mongoTemplate.save(objectToSave);
          newObjectSaved = true;
        } else {
          savedObject = alreadySavedObject;
        }
      }
      processGitBranchMetadata(objectToSave, changeType, entityDetail, scmPushResponse, objectIdOfYaml,
          entityGitBranchMetadata, newObjectSaved);

      gitSyncMsvcHelper.postPushInformationToGitMsvc(entityDetail, scmPushResponse, gitBranchInfo);
    } else {
      savedObject = mongoTemplate.save(objectToSave);
    }
    return setBranchInObject(savedObject);
  }

  private <B extends GitSyncableEntity> void processGitBranchMetadata(B objectToSave, ChangeType changeType,
      EntityDetail entityDetail, ScmPushResponse scmPushResponse, String objectIdOfYaml,
      EntityGitBranchMetadata entityGitBranchMetadata, boolean newObjectSaved) {
    removeOldEntityGitBranchMetadata(entityDetail, scmPushResponse);
    // If change type is delete wee have already pulled this branch from entity git branch metadata hence nothing else
    // needs to be done. if entity git branch metadata exists for same object id push a branch to it if new save new
    // object.
    if (changeType != ChangeType.DELETE) {
      if (entityGitBranchMetadata == null) {
        if (!newObjectSaved) {
          throw new UnexpectedException("Git branch metadata is null but no new object saved.");
        }
        saveEntityGitBranchMetadata(objectToSave, objectIdOfYaml, entityDetail, scmPushResponse);
      } else {
        if (newObjectSaved) {
          saveEntityGitBranchMetadata(objectToSave, objectIdOfYaml, entityDetail, scmPushResponse);
        } else {
          pushNewBranchToEntityGitBranchMetadata(scmPushResponse, entityGitBranchMetadata);
        }
      }
    }
  }

  private void pushNewBranchToEntityGitBranchMetadata(
      ScmPushResponse scmPushResponse, EntityGitBranchMetadata entityGitBranchMetadata) {
    // doing find and modify so that we dont run into mongo versioning issue often.
    final Query findQuery =
        query(Criteria.where(EntityGitBranchMetadataKeys.uuid).is(entityGitBranchMetadata.getUuid()));
    Update update = new Update().push(EntityGitBranchMetadataKeys.branch, scmPushResponse.getBranch());
    if (scmPushResponse.isPushToDefaultBranch()) {
      update.set(EntityGitBranchMetadataKeys.isDefault, true);
    }
    gitBranchingHelper.findAndModify(findQuery, update);
  }

  private void removeOldEntityGitBranchMetadata(EntityDetail entityDetail, ScmPushResponse scmPushResponse) {
    final Query query = query(Criteria.where(EntityGitBranchMetadataKeys.entityFqn)
                                  .is(entityDetail.getEntityRef().getFullyQualifiedName())
                                  .and(EntityGitBranchMetadataKeys.entityType)
                                  .is(entityDetail.getType().getYamlName())
                                  .and(EntityGitBranchMetadataKeys.accountId)
                                  .is(entityDetail.getEntityRef().getAccountIdentifier())
                                  .and(EntityGitBranchMetadataKeys.yamlGitConfigId)
                                  .is(scmPushResponse.getYamlGitConfigId())
                                  .and(EntityGitBranchMetadataKeys.branch)
                                  .is(scmPushResponse.getBranch()));
    Update update = new Update().pull(EntityGitBranchMetadataKeys.branch, scmPushResponse.getBranch());
    if (scmPushResponse.isPushToDefaultBranch()) {
      update.set(EntityGitBranchMetadataKeys.isDefault, false);
    }
    // doing find and modify so that we dont run into mongo versioning issue often.
    gitBranchingHelper.findAndModify(query, update);
  }

  private EntityGitBranchMetadata getEntityGitBranchMetadata(
      EntityDetail entityDetail, ScmPushResponse scmPushResponse, String objectIdOfYaml) {
    return mongoTemplate.findOne(query(Criteria.where(EntityGitBranchMetadataKeys.entityFqn)
                                           .is(entityDetail.getEntityRef().getFullyQualifiedName())
                                           .and(EntityGitBranchMetadataKeys.entityType)
                                           .is(entityDetail.getType().getYamlName())
                                           .and(EntityGitBranchMetadataKeys.accountId)
                                           .is(entityDetail.getEntityRef().getAccountIdentifier())
                                           .and(EntityGitBranchMetadataKeys.yamlGitConfigId)
                                           .is(scmPushResponse.getYamlGitConfigId())
                                           .and(EntityGitBranchMetadataKeys.objectId)
                                           .is(objectIdOfYaml)),
        EntityGitBranchMetadata.class);
  }

  private <B extends GitSyncableEntity, Y extends YamlDTO> void saveEntityGitBranchMetadata(
      B objectToSave, String objectIdOfYaml, EntityDetail entityDetail, ScmPushResponse scmPushResponse) {
    gitBranchingHelper.save(EntityGitBranchMetadata.builder()
                                .objectId(objectIdOfYaml)
                                .accountId(entityDetail.getEntityRef().getAccountIdentifier())
                                .orgIdentifier(objectToSave.getOrgIdentifier())
                                .projectIdentifier(objectToSave.getProjectIdentifier())
                                .branch(Arrays.asList(scmPushResponse.getBranch()))
                                .entityFqn(entityDetail.getEntityRef().getFullyQualifiedName())
                                .objectId(objectIdOfYaml)
                                .isDefault(scmPushResponse.isPushToDefaultBranch())
                                .entityType(entityDetail.getType().getYamlName())
                                .yamlGitConfigId(scmPushResponse.getYamlGitConfigId())
                                .uuidOfEntity(objectToSave.getUuid())
                                .build());
  }

  private boolean isGitSyncEnabled(String projectIdentifier, String orgIdentifier, String accountIdentifier) {
    try {
      return entityKeySource.fetchKey(buildEntityScopeInfo(projectIdentifier, orgIdentifier, accountIdentifier));
    } catch (Exception ex) {
      log.error("Exception while communicating to the git sync service", ex);
      return false;
    }
  }

  EntityScopeInfo buildEntityScopeInfo(String projectIdentifier, String orgIdentifier, String accountId) {
    final EntityScopeInfo.Builder entityScopeInfoBuilder = EntityScopeInfo.newBuilder().setAccountId(accountId);
    if (!isEmpty(projectIdentifier)) {
      entityScopeInfoBuilder.setProjectId(StringValue.of(projectIdentifier));
    }
    if (!isEmpty(orgIdentifier)) {
      entityScopeInfoBuilder.setOrgId(StringValue.of(orgIdentifier));
    }
    return entityScopeInfoBuilder.build();
  }

  private <B extends GitSyncableEntity> EntityType getEntityType(Class<B> entityClass) {
    return gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName()).getEntityType();
  }

  private <B extends GitSyncableEntity> B setBranchInObject(B object) {
    // todo(abhinav): in list api when pipeline asks for connector from different branches something extra needs to be
    // done.

    final GitEntityInfo gitEntityInfo = GitSyncBranchThreadLocal.get();
    if (object != null && gitEntityInfo != null && gitEntityInfo.getBranch() != null
        && !gitEntityInfo.getBranch().equals(DEFAULT_BRANCH)) {
      object.setBranch(gitEntityInfo.getBranch());
    }
    return object;
  }
}
