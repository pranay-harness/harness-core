package io.harness.gitsync.persistance;

import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;
import io.harness.gitsync.InfoForPush;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.interceptor.GitBranchInfo;
import io.harness.gitsync.interceptor.GitSyncBranchThreadLocal;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailRestToProtoMapper;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class GitAwarePersistenceImpl implements GitAwarePersistence {
  private final MongoTemplate mongoTemplate;
  private final SCMHelper scmHelper;
  private final HarnessToGitPushInfoServiceBlockingStub harnessToGitPushInfoServiceBlockingStub;
  private final EntityDetailRestToProtoMapper entityDetailRestToProtoMapper;
  private final KryoSerializer kryoSerializer;

  // todo(abhinav): Add branching logic for find.

  @Override
  public <B> B findAndModify(@NotNull Query query, @NotNull Update update, @NotNull Class<B> entityClass) {
    getAndSetBranchInfo(query, entityClass);
    return mongoTemplate.findAndModify(query, update, entityClass);
  }

  @Override
  public <B> B findOne(@NotNull Query query, @NotNull Class<B> entityClass) {
    getAndSetBranchInfo(query, entityClass);
    return mongoTemplate.findOne(query, entityClass);
  }

  @Override
  public <B> List<B> find(@NotNull Query query, @NotNull Class<B> entityClass) {
    getAndSetBranchInfo(query, entityClass);
    return mongoTemplate.find(query, entityClass);
  }

  @Override
  public <B> List<B> findDistinct(
      @NotNull Query query, @NotNull String field, @NotNull Class<?> entityClass, @NotNull Class<B> resultClass) {
    getAndSetBranchInfo(query, entityClass);
    return mongoTemplate.findDistinct(query, field, entityClass, resultClass);
  }

  @Override
  public <Y> UpdateResult upsert(@NotNull Query query, @NotNull Update update, @NotNull Class<?> entityClass, Y yaml) {
    getAndSetBranchInfo(query, entityClass);
    // todo: handle changetype upsert.
    return mongoTemplate.upsert(query, update, entityClass);
  }

  @Override
  public <Y> UpdateResult updateFirst(
      @NotNull Query query, @NotNull Update update, @NotNull Class<?> entityClass, Y yaml) {
    getAndSetBranchInfo(query, entityClass);
    //    scmHelper.pushToGit(yaml, ChangeType.MODIFY);
    return mongoTemplate.updateFirst(query, update, entityClass);
  }

  @Override
  public <Y> DeleteResult remove(@NotNull GitSyncableEntity object, @NotNull String collectionName, Y yaml) {
    getAndSetBranchInfo(object);
    //    scmHelper.pushToGit(yaml, ChangeType.DELETE);
    return mongoTemplate.remove(object, collectionName);
  }

  @Override
  public <Y> DeleteResult remove(@NotNull Object object, Y yaml) {
    getAndSetBranchInfo(object);
    //    scmHelper.pushToGit(yaml, ChangeType.DELETE);
    return mongoTemplate.remove(object);
  }

  @Override
  public <T extends GitSyncableEntity, Y> T save(T objectToSave, Y yaml, ChangeType changeType) {
    getAndSetBranchInfo(objectToSave);
    final GitBranchInfo gitBranchInfo = GitSyncBranchThreadLocal.get();
    final EntityDetail entityDetail = objectToSave.getEntityDetail();
    final InfoForGitPush infoForPush = getInfoForPush(gitBranchInfo, entityDetail);
    String commitId = scmHelper.pushToGit(yaml, changeType, infoForPush.getScmConnector(), infoForPush.getFilePath());
    final T savedObject = mongoTemplate.save(objectToSave);
    harnessToGitPushInfoServiceBlockingStub.pushFromHarness(
        PushInfo.newBuilder()
            .setAccountId(entityDetail.getEntityRef().getAccountIdentifier())
            .setCommitId(commitId)
            .setEntityDetail(entityDetailRestToProtoMapper.createEntityDetailDTO(entityDetail))
            .setFilePath(infoForPush.getFilePath())
            .setYamlGitConfigId(gitBranchInfo.getYamlGitConfigId())
            .build());
    return savedObject;
  }

  @Override
  public <T extends GitSyncableEntity, Y> T save(T objectToSave, Y yaml) {
    return save(objectToSave, yaml, ChangeType.ADD);
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

  @Override
  public <T extends GitSyncableEntity, Y> T insert(T objectToSave, Y yaml) {
    getAndSetBranchInfo(objectToSave);
    //    scmHelper.pushToGit(yaml, ChangeType.ADD);
    return mongoTemplate.insert(objectToSave);
  }

  @Override
  public <T extends GitSyncableEntity, Y> T insert(T objectToSave, String collectionName, Y yaml) {
    getAndSetBranchInfo(objectToSave);
    //    scmHelper.pushToGit(yaml, ChangeType.ADD);
    return mongoTemplate.insert(objectToSave, collectionName);
  }

  private <T> void getAndSetBranchInfo(T objectToSave) {
    if (objectToSave instanceof GitSyncableEntity) {
      final GitBranchInfo gitBranchInfo = GitSyncBranchThreadLocal.get();
      final String branch = gitBranchInfo.getBranch();
      ((GitSyncableEntity) objectToSave).setBranch(branch);
    }
  }

  private <T> void getAndSetBranchInfo(Query q, Class<T> entityClass) {
    if (GitSyncableEntity.class.isAssignableFrom(entityClass)) {
      final GitBranchInfo gitBranchInfo = GitSyncBranchThreadLocal.get();
      final String branch = gitBranchInfo.getBranch();
      q.addCriteria(Criteria.where("branch").is(branch));
    }
  }
}
