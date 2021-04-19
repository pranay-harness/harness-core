package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.branching.EntityGitBranchMetadata;
import io.harness.gitsync.branching.EntityGitBranchMetadata.EntityGitBranchMetadataKeys;
import io.harness.gitsync.branching.GitBranchingHelper;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchThreadLocal;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.gitsync.persistance.GitSyncableEntity.GitSyncableEntityKeys;
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
        updateCriteriaIfGitSyncEnabled(projectIdentifier, orgIdentifier, accountId, getEntityType(entityClass));
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
        updateCriteriaIfGitSyncEnabled(projectIdentifier, orgIdentifier, accountId, getEntityType(entityClass));
    // todo(abhinav): do we have to do anything extra if git sync is not there?
    List<Criteria> criteriaList = Arrays.asList(criteria, gitSyncCriteria);
    Query query =
        new Query().addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
    final B object = mongoTemplate.findOne(query, entityClass);
    return Optional.ofNullable(object);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> List<B> find(@NotNull Criteria criteria, Pageable pageable,
      String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    final Criteria gitSyncCriteria =
        updateCriteriaIfGitSyncEnabled(projectIdentifier, orgIdentifier, accountId, getEntityType(entityClass));
    // todo(abhinav): do we have to do anything extra if git sync is not there?
    List<Criteria> criteriaList = Arrays.asList(criteria, gitSyncCriteria);
    Query query = new Query()
                      .addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])))
                      .with(pageable);
    return mongoTemplate.find(query, entityClass);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> B findAndModify(Criteria criteria, Update update,
      ChangeType changeType, String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    final Criteria gitSyncCriteria =
        updateCriteriaIfGitSyncEnabled(projectIdentifier, orgIdentifier, accountId, getEntityType(entityClass));
    List<Criteria> criteriaList = Arrays.asList(criteria, gitSyncCriteria);
    Query query =
        new Query().addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
    // todo(abhinav): do we have to do anything extra if git sync is not there?
    final B object = mongoTemplate.findOne(query, entityClass);
    if (object == null) {
      return null;
    }
    return update(query, update, changeType, projectIdentifier, orgIdentifier, accountId, entityClass);
  }

  // In this method it is assumed that project id, org id and account id will not be updated for the entity and criteria
  // object has been updated.
  private <B extends GitSyncableEntity, Y extends YamlDTO> B update(Query query, Update update, ChangeType changeType,
      String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    // todo(abhinav): do we have to do anything extra if git sync is not there?
    final B objectToUpdate = mongoTemplate.findOne(query, entityClass);
    if (objectToUpdate == null) {
      return null;
    }

    final GitEntityInfo gitBranchInfo = GitSyncBranchThreadLocal.get();
    final EntityDetail entityDetail =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName()).getEntityDetail(objectToUpdate);

    if (changeType != ChangeType.NONE && isGitSyncEnabled(projectIdentifier, orgIdentifier, accountId)) {
      final String yamlString = EntityToYamlStringUtils.getYamlString(
          objectToUpdate, gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName()));
      final ScmPushResponse scmPushResponse =
          scmGitSyncHelper.pushToGit(gitBranchInfo, yamlString, changeType, entityDetail);
      final String objectIdOfYaml = scmPushResponse.getObjectId();
      final EntityGitBranchMetadata entityGitBranchMetadata =
          getEntityGitBranchMetadata(entityDetail, scmPushResponse, objectIdOfYaml);
      // todo(abhinav): do not hardcode.
      update.addToSet(GitSyncableEntityKeys.objectIdOfYaml, objectIdOfYaml);
      update.addToSet(GitSyncableEntityKeys.isFromDefaultBranch, scmPushResponse.isPushToDefaultBranch());
      update.addToSet(GitSyncableEntityKeys.yamlGitConfigRef, scmPushResponse.getYamlGitConfigId());
      final B modifiedObject =
          mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), entityClass);
      processGitBranchMetadata(modifiedObject, changeType, gitBranchInfo, entityDetail, scmPushResponse, objectIdOfYaml,
          entityGitBranchMetadata);
      gitSyncMsvcHelper.postPushInformationToGitMsvc(entityDetail, scmPushResponse);
    } else {
      update.addToSet(GitSyncableEntityKeys.isFromDefaultBranch, true);
    }
    return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), entityClass);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> boolean exists(@NotNull Criteria criteria,
      String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    final Criteria gitSyncCriteria =
        updateCriteriaIfGitSyncEnabled(projectIdentifier, orgIdentifier, accountId, getEntityType(entityClass));
    List<Criteria> criteriaList = Arrays.asList(criteria, gitSyncCriteria);
    Query query =
        new Query().addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
    // todo(abhinav): do we have to do anything extra if git sync is not there?
    return mongoTemplate.exists(query, entityClass);
  }

  private Criteria updateCriteriaIfGitSyncEnabled(
      String projectIdentifier, String orgIdentifier, String accountId, EntityType entityType) {
    if (isGitSyncEnabled(projectIdentifier, orgIdentifier, accountId)) {
      //
      final GitEntityInfo gitBranchInfo = GitSyncBranchThreadLocal.get();
      final List<String> objectId;
      if (gitBranchInfo == null || gitBranchInfo.getYamlGitConfigId() == null || gitBranchInfo.getBranch() == null
          || gitBranchInfo.getYamlGitConfigId().equals(GitSyncConstants.DEFAULT_BRANCH)
          || gitBranchInfo.getBranch().equals(GitSyncConstants.DEFAULT_BRANCH)) {
        return new Criteria().andOperator(
            new Criteria().orOperator(Criteria.where(GitSyncableEntityKeys.isFromDefaultBranch).is(true),
                Criteria.where(GitSyncableEntityKeys.isFromDefaultBranch).exists(false)));
      } else {
        objectId = gitBranchingHelper.getObjectIdForYamlGitConfigBranchAndScope(gitBranchInfo.getYamlGitConfigId(),
            gitBranchInfo.getBranch(), projectIdentifier, orgIdentifier, accountId, entityType);
        return new Criteria().and(GitSyncableEntityKeys.objectIdOfYaml).in(objectId);
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
    final EntityDetail entityDetail =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName()).getEntityDetail(objectToSave);
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

      savedObject = mongoTemplate.save(objectToSave);

      processGitBranchMetadata(objectToSave, changeType, gitBranchInfo, entityDetail, scmPushResponse, objectIdOfYaml,
          entityGitBranchMetadata);

      gitSyncMsvcHelper.postPushInformationToGitMsvc(entityDetail, scmPushResponse);
    } else {
      savedObject = mongoTemplate.save(objectToSave);
    }
    return savedObject;
  }

  private <B extends GitSyncableEntity> void processGitBranchMetadata(B objectToSave, ChangeType changeType,
      GitEntityInfo gitBranchInfo, EntityDetail entityDetail, ScmPushResponse scmPushResponse, String objectIdOfYaml,
      EntityGitBranchMetadata entityGitBranchMetadata) {
    removeOldEntityGitBranchMetadata(gitBranchInfo, entityDetail, scmPushResponse);
    // If change type is delete wee have already pulled this branch from entity git branch metadata hence nothing else
    // needs to be done. if entity git branch metadata exists for same object id push a branch to it if new save new
    // object.
    if (changeType != ChangeType.DELETE) {
      if (entityGitBranchMetadata == null) {
        saveEntityGitBranchMetadata(objectToSave, objectIdOfYaml, entityDetail, scmPushResponse);
      } else {
        pushNewBranchToEntityGitBranchMetadata(scmPushResponse, entityGitBranchMetadata);
      }
    }
  }

  private void pushNewBranchToEntityGitBranchMetadata(
      ScmPushResponse scmPushResponse, EntityGitBranchMetadata entityGitBranchMetadata) {
    // doing find and modify so that we dont run into mongo versioning issue often.
    final Query findQuery =
        query(Criteria.where(EntityGitBranchMetadataKeys.uuid).is(entityGitBranchMetadata.getUuid()));
    Update update = new Update().push(EntityGitBranchMetadataKeys.branch, scmPushResponse.getBranch());
    gitBranchingHelper.findAndModify(findQuery, update);
  }

  private void removeOldEntityGitBranchMetadata(
      GitEntityInfo gitBranchInfo, EntityDetail entityDetail, ScmPushResponse scmPushResponse) {
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

    // doing find and modify so that we dont run into mongo versioning issue often.
    gitBranchingHelper.findAndModify(query, update);
  }

  private <B extends GitSyncableEntity> B getAlreadySavedObject(
      B objectToSave, String objectIdOfYaml, Class<B> entityClass) {
    // todo(abhinav): find way to not hardcode keys;

    return mongoTemplate.findOne(query(Criteria.where(GitSyncableEntityKeys.objectIdOfYaml)
                                           .is(objectIdOfYaml)
                                           .and("identifier")
                                           .is(objectToSave.getIdentifier())
                                           .and("projectIdentifier")
                                           .is(objectToSave.getProjectIdentifier())
                                           .and("orgIdentifier")
                                           .is(objectToSave.getOrgIdentifier())
                                           .and("accountIdentifier")
                                           .is(objectToSave.getAccountIdentifier())),
        entityClass);
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
}
