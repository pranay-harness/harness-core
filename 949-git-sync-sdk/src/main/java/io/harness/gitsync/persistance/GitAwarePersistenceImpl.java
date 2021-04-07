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
import io.harness.gitsync.entityInfo.EntityGitPersistenceHelperService;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchThreadLocal;
import io.harness.gitsync.scm.SCMGitSyncHelper;
import io.harness.gitsync.scm.beans.ScmPushResponse;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.utils.NGYamlUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.StringValue;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Slf4j
@Singleton
@OwnedBy(DX)
public class GitAwarePersistenceImpl<B extends GitSyncableEntity, Y extends YamlDTO>
    implements GitAwarePersistence<B, Y> {
  private Class<B> entityClass;
  private Class<Y> yamlClass;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private EntityKeySource entityKeySource;
  @Inject private GitBranchingHelper gitBranchingHelper;
  @Inject private Map<String, EntityGitPersistenceHelperService> gitPersistenceHelperServiceMap;
  @Inject private SCMGitSyncHelper scmGitSyncHelper;
  @Inject private GitSyncMsvcHelper gitSyncMsvcHelper;

  public GitAwarePersistenceImpl(Class<B> entityClass, Class<Y> yamlClass) {
    this.entityClass = entityClass;
    this.yamlClass = yamlClass;
  }

  @Override
  public List<B> find(@NotNull Query query, String projectIdentifier, String orgIdentifier, String accountId) {
    if (isGitSyncEnabled(projectIdentifier, orgIdentifier, accountId)) {
      //
      final GitEntityInfo gitBranchInfo = GitSyncBranchThreadLocal.get();
      final List<String> objectId;
      if (gitBranchInfo == null || gitBranchInfo.getYamlGitConfigId() == null || gitBranchInfo.getBranch() == null) {
        objectId = gitBranchingHelper.getObjectIdForDefaultBranchAndScope(
            projectIdentifier, orgIdentifier, accountId, getEntityType());
      } else {
        objectId = gitBranchingHelper.getObjectIdForYamlGitConfigBranchAndScope(gitBranchInfo.getYamlGitConfigId(),
            gitBranchInfo.getBranch(), projectIdentifier, orgIdentifier, accountId, getEntityType());
      }

      // todo(abhinav): find way to not hardcode objectId;
      query.addCriteria(Criteria.where("objectId").in(objectId));
      return mongoTemplate.find(query, entityClass);
    }
    // todo(abhinav): do we have to do anything extra if git sync is not there?
    return mongoTemplate.find(query, entityClass);
  }

  @Override
  public B save(B objectToSave, Y yaml) {
    return save(objectToSave, yaml, ChangeType.ADD);
  }

  @Override
  public B save(B objectToSave, Y yaml, ChangeType changeType) {
    final GitEntityInfo gitBranchInfo = GitSyncBranchThreadLocal.get();
    final EntityDetail entityDetail =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName()).getEntityDetail(objectToSave);
    B savedObject;
    if (isGitSyncEnabled(entityDetail.getEntityRef().getProjectIdentifier(),
            entityDetail.getEntityRef().getOrgIdentifier(), entityDetail.getEntityRef().getAccountIdentifier())) {
      final String yamlString = NGYamlUtils.getYamlString(yaml);

      final ScmPushResponse scmPushResponse =
          scmGitSyncHelper.pushToGit(gitBranchInfo, yamlString, changeType, entityDetail);

      final String objectIdOfYaml = scmPushResponse.getObjectId();
      objectToSave.setObjectIdOfYaml(objectIdOfYaml);

      final EntityGitBranchMetadata entityGitBranchMetadata =
          getEntityGitBranchMetadata(gitBranchInfo, entityDetail, scmPushResponse, objectIdOfYaml);

      savedObject = mongoTemplate.save(objectToSave);

      processGitBranchMetadata(objectToSave, changeType, gitBranchInfo, entityDetail, scmPushResponse, objectIdOfYaml,
          entityGitBranchMetadata);

      gitSyncMsvcHelper.postPushInformationToGitMsvc(gitBranchInfo, entityDetail, scmPushResponse);
    } else {
      savedObject = mongoTemplate.save(objectToSave);
    }
    return savedObject;
  }

  private void processGitBranchMetadata(B objectToSave, ChangeType changeType, GitEntityInfo gitBranchInfo,
      EntityDetail entityDetail, ScmPushResponse scmPushResponse, String objectIdOfYaml,
      EntityGitBranchMetadata entityGitBranchMetadata) {
    if (changeType != ChangeType.DELETE) {
      if (entityGitBranchMetadata == null) {
        saveEntityGitBranchMetadata(objectToSave, objectIdOfYaml, gitBranchInfo, entityDetail, scmPushResponse);
      } else {
        entityGitBranchMetadata.setObjectId(objectIdOfYaml);
        mongoTemplate.save(entityGitBranchMetadata);
      }
    } else {
      if (entityGitBranchMetadata != null) {
        mongoTemplate.remove(entityGitBranchMetadata);
      } else {
        log.error("Expected entity git branch metadata for {}", objectToSave);
      }
    }
  }

  private B getAlreadySavedObject(B objectToSave, String objectIdOfYaml) {
    // todo(abhinav): find way to not hardcode keys;

    return mongoTemplate.findOne(query(Criteria.where("objectIdOfYaml")
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
      GitEntityInfo gitBranchInfo, EntityDetail entityDetail, ScmPushResponse scmPushResponse, String objectIdOfYaml) {
    return mongoTemplate.findOne(query(Criteria.where(EntityGitBranchMetadataKeys.entityFqn)
                                           .is(entityDetail.getEntityRef().getFullyQualifiedName())
                                           .and(EntityGitBranchMetadataKeys.entityType)
                                           .is(entityDetail.getType().name())
                                           .and(EntityGitBranchMetadataKeys.accountId)
                                           .is(gitBranchInfo.getAccountId())
                                           .is(scmPushResponse.getYamlGitConfigId())
                                           .and(EntityGitBranchMetadataKeys.objectId)
                                           .is(objectIdOfYaml)),
        EntityGitBranchMetadata.class);
  }

  private void saveEntityGitBranchMetadata(B objectToSave, String objectIdOfYaml, GitEntityInfo gitBranchInfo,
      EntityDetail entityDetail, ScmPushResponse scmPushResponse) {
    mongoTemplate
        .save(EntityGitBranchMetadata.builder()
                  .objectId(objectIdOfYaml)
                  .accountId(gitBranchInfo.getAccountId())
                  .orgIdentifier(objectToSave.getOrgIdentifier()))
        .projectIdentifier(objectToSave.getProjectIdentifier())
        .branch(gitBranchInfo.getBranch())
        .entityFqn(entityDetail.getEntityRef().getFullyQualifiedName())
        .objectId(objectIdOfYaml)
        .isDefault(scmPushResponse.isPushToDefaultBranch())
        .entityType(entityDetail.getType().name())
        .yamlGitConfigId(scmPushResponse.getYamlGitConfigId())
        .build();
  }

  private boolean isGitSyncEnabled(String projectIdentifier, String orgIdentifier, String accountIdentifier) {
    return entityKeySource.fetchKey(buildEntityScopeInfo(projectIdentifier, orgIdentifier, accountIdentifier));
  }

  EntityScopeInfo buildEntityScopeInfo(String projectIdentifier, String orgIdentifier, String accountId) {
    final EntityScopeInfo.Builder entityScopeInfoBuilder = EntityScopeInfo.newBuilder().setAccountId(accountId);
    if (!isEmpty(projectIdentifier)) {
      entityScopeInfoBuilder.setProjectId(StringValue.of(projectIdentifier));
    }
    if (!isEmpty(projectIdentifier)) {
      entityScopeInfoBuilder.setOrgId(StringValue.of(orgIdentifier));
    }
    return entityScopeInfoBuilder.build();
  }

  private EntityType getEntityType() {
    return gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName()).getEntityType();
  }
}
