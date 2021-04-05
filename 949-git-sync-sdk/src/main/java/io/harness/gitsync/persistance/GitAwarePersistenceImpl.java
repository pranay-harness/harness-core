package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;
import io.harness.gitsync.InfoForPush;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.branching.GitBranchingHelper;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.entityInfo.EntityGitPersistenceHelperService;
import io.harness.gitsync.interceptor.GitBranchInfo;
import io.harness.gitsync.interceptor.GitSyncBranchThreadLocal;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailRestToProtoMapper;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.service.ScmClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.StringValue;
import com.mongodb.client.result.DeleteResult;
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
  Class<B> entityClass;
  Class<Y> yamlClass;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private HarnessToGitPushInfoServiceBlockingStub harnessToGitPushInfoServiceBlockingStub;
  @Inject private EntityDetailRestToProtoMapper entityDetailRestToProtoMapper;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private ScmClient scmClient;
  @Inject private EntityKeySource entityKeySource;
  @Inject private GitBranchingHelper gitBranchingHelper;
  @Inject private Map<String, EntityGitPersistenceHelperService> gitPersistenceHelperServiceMap;

  public GitAwarePersistenceImpl(Class<B> entityClass, Class<Y> yamlClass) {
    this.entityClass = entityClass;
    this.yamlClass = yamlClass;
  }

  @Override
  public List<B> find(@NotNull Query query, String projectIdentifier, String orgIdentifier, String accountId) {
    if (entityKeySource.fetchKey(buildEntityScopeInfo(projectIdentifier, orgIdentifier, accountId))) {
      //
      final GitBranchInfo gitBranchInfo = GitSyncBranchThreadLocal.get();
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
  public DeleteResult remove(@NotNull B object, Y yaml) {
    //    scmHelper.pushToGit(yaml, ChangeType.DELETE);
    return mongoTemplate.remove(object);
  }

  @Override
  public B save(B objectToSave, Y yaml, ChangeType changeType) {
    final GitBranchInfo gitBranchInfo = GitSyncBranchThreadLocal.get();
    final EntityDetail entityDetail =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName()).getEntityDetail(objectToSave);
    B savedObject;
    if (entityKeySource.fetchKey(buildEntityScopeInfo(entityDetail.getEntityRef().getProjectIdentifier(),
            entityDetail.getEntityRef().getOrgIdentifier(), entityDetail.getEntityRef().getAccountIdentifier()))) {
      final InfoForGitPush infoForPush = getInfoForPush(gitBranchInfo, entityDetail);
      final CreateFileResponse createFileResponse = doScmPush(yaml, gitBranchInfo, infoForPush);
      savedObject = mongoTemplate.save(objectToSave);
      // todo(abhinav): createFileResponse get object id and other things and save in EntityBranchMetadata here.
      postPushInformationToGitMsvc(gitBranchInfo, entityDetail, infoForPush);
    } else {
      savedObject = mongoTemplate.save(objectToSave);
    }
    return savedObject;
  }

  @Override
  public B save(B objectToSave, Y yaml) {
    return save(objectToSave, yaml, ChangeType.ADD);
  }

  private void postPushInformationToGitMsvc(
      GitBranchInfo gitBranchInfo, EntityDetail entityDetail, InfoForGitPush infoForPush) {
    harnessToGitPushInfoServiceBlockingStub.pushFromHarness(
        PushInfo.newBuilder()
            .setAccountId(entityDetail.getEntityRef().getAccountIdentifier())
            .setCommitId("commitId")
            .setEntityDetail(entityDetailRestToProtoMapper.createEntityDetailDTO(entityDetail))
            .setFilePath(infoForPush.getFilePath())
            .setYamlGitConfigId(gitBranchInfo.getYamlGitConfigId())
            .build());
  }

  private CreateFileResponse doScmPush(Y yaml, GitBranchInfo gitBranchInfo, InfoForGitPush infoForPush) {
    final GitFileDetails gitFileDetails = GitFileDetails.builder()
                                              .branch(gitBranchInfo.getBranch())
                                              .commitMessage("test")
                                              .fileContent(yaml.toString())
                                              .filePath(gitBranchInfo.getFilePath())
                                              .build();
    return scmClient.createFile(infoForPush.getScmConnector(), gitFileDetails);
  }

  private InfoForGitPush getInfoForPush(GitBranchInfo gitBranchInfo, EntityDetail entityDetail) {
    final InfoForPush pushInfo = harnessToGitPushInfoServiceBlockingStub.getConnectorInfo(
        FileInfo.newBuilder()
            .setAccountId(gitBranchInfo.getAccountId())
            .setBranch(gitBranchInfo.getBranch())
            .setFilePath(gitBranchInfo.getFilePath())
            .setYamlGitConfigId(gitBranchInfo.getYamlGitConfigId())
            .setEntityDetail(entityDetailRestToProtoMapper.createEntityDetailDTO(entityDetail))
            .build());
    if (!pushInfo.getStatus()) {
      if (pushInfo.getException().getValue() == null) {
        throw new InvalidRequestException("Unknown exception occurred");
      }
      throw(WingsException) kryoSerializer.asObject(pushInfo.getException().getValue().toByteArray());
    }
    final ScmConnector scmConnector =
        (ScmConnector) kryoSerializer.asObject(pushInfo.getConnector().getValue().toByteArray());
    return InfoForGitPush.builder().filePath(pushInfo.getFilePath().getValue()).scmConnector(scmConnector).build();
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
