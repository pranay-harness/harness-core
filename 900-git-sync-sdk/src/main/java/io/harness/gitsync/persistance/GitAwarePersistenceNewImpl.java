package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.interceptor.GitSyncConstants.DEFAULT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.scm.SCMGitSyncHelper;
import io.harness.gitsync.scm.beans.ScmPushResponse;
import io.harness.manage.GlobalContextManager;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.utils.NGYamlUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Singleton
@OwnedBy(DX)
@Slf4j
public class GitAwarePersistenceNewImpl implements GitAwarePersistence {
  private MongoTemplate mongoTemplate;
  private GitSyncSdkService gitSyncSdkService;
  private Map<String, GitSdkEntityHandlerInterface> gitPersistenceHelperServiceMap;
  private SCMGitSyncHelper scmGitSyncHelper;
  private GitSyncMsvcHelper gitSyncMsvcHelper;
  private ObjectMapper objectMapper;

  @Inject
  public GitAwarePersistenceNewImpl(MongoTemplate mongoTemplate, GitSyncSdkService gitSyncSdkService,
      Map<String, GitSdkEntityHandlerInterface> gitPersistenceHelperServiceMap, SCMGitSyncHelper scmGitSyncHelper,
      GitSyncMsvcHelper gitSyncMsvcHelper, @Named("GitSyncObjectMapper") ObjectMapper objectMapper) {
    this.mongoTemplate = mongoTemplate;
    this.gitSyncSdkService = gitSyncSdkService;
    this.gitPersistenceHelperServiceMap = gitPersistenceHelperServiceMap;
    this.scmGitSyncHelper = scmGitSyncHelper;
    this.gitSyncMsvcHelper = gitSyncMsvcHelper;
    this.objectMapper = objectMapper;
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> B save(
      B objectToSave, Y yaml, ChangeType changeType, Class<B> entityClass) {
    final GitSdkEntityHandlerInterface gitSdkEntityHandlerInterface =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName());
    final EntityDetail entityDetail = gitSdkEntityHandlerInterface.getEntityDetail(objectToSave);
    if (changeType != ChangeType.NONE
        && isGitSyncEnabled(entityDetail.getEntityRef().getProjectIdentifier(),
            entityDetail.getEntityRef().getOrgIdentifier(), entityDetail.getEntityRef().getAccountIdentifier())) {
      final GitEntityInfo gitBranchInfo = getGitEntityInfo();
      final String yamlString = NGYamlUtils.getYamlString(yaml, objectMapper);
      final ScmPushResponse scmPushResponse =
          scmGitSyncHelper.pushToGit(gitBranchInfo, yamlString, changeType, entityDetail);

      updateObjectWithGitMetadata(objectToSave, scmPushResponse);
      final B savedObjectInMongo = mongoTemplate.save(objectToSave);

      gitSyncMsvcHelper.postPushInformationToGitMsvc(entityDetail, scmPushResponse, gitBranchInfo);
      return savedObjectInMongo;
    }
    if (changeType == ChangeType.ADD) {
      objectToSave.setIsFromDefaultBranch(true);
    }
    return mongoTemplate.save(objectToSave);
  }

  private <B extends GitSyncableEntity> void updateObjectWithGitMetadata(
      B objectToSave, ScmPushResponse scmPushResponse) {
    final String objectIdOfYaml = scmPushResponse.getObjectId();
    objectToSave.setObjectIdOfYaml(objectIdOfYaml);
    objectToSave.setYamlGitConfigRef(scmPushResponse.getYamlGitConfigId());
    objectToSave.setIsFromDefaultBranch(scmPushResponse.isPushToDefaultBranch());
    objectToSave.setBranch(scmPushResponse.getBranch());
    objectToSave.setFilePath(scmPushResponse.getFilePath());
    objectToSave.setRootFolder(scmPushResponse.getFolderPath());
  }

  private GitEntityInfo getGitEntityInfo() {
    final GitSyncBranchContext gitSyncBranchContext =
        GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);
    if (gitSyncBranchContext == null) {
      log.error("Git branch context set as null even git sync is enabled");
      // Setting to default branch in case it is not set.
      return GitEntityInfo.builder().yamlGitConfigId(DEFAULT).branch(DEFAULT).build();
    }
    return gitSyncBranchContext.getGitBranchInfo();
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> Long count(
      Criteria criteria, String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    final Criteria gitSyncCriteria =
        updateCriteriaIfGitSyncEnabled(projectIdentifier, orgIdentifier, accountId, entityClass);
    List<Criteria> criteriaList = Arrays.asList(criteria, gitSyncCriteria);
    Query query = new Query()
                      .addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])))
                      .limit(-1)
                      .skip(-1);

    return mongoTemplate.count(query, entityClass);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> Optional<B> findOne(
      Criteria criteria, String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    final Criteria gitSyncCriteria =
        updateCriteriaIfGitSyncEnabled(projectIdentifier, orgIdentifier, accountId, entityClass);
    List<Criteria> criteriaList = Arrays.asList(criteria, gitSyncCriteria);
    Query query =
        new Query().addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
    final B object = mongoTemplate.findOne(query, entityClass);
    return Optional.ofNullable(object);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> Optional<B> findOne(
      Criteria criteria, String repo, String branch, Class<B> entityClass) {
    final Criteria gitSyncCriteria = createGitSyncCriteriaForRepoAndBranch(repo, branch, null, entityClass);
    List<Criteria> criteriaList = Arrays.asList(criteria, gitSyncCriteria);
    Query query =
        new Query().addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
    final B object = mongoTemplate.findOne(query, entityClass);
    return Optional.ofNullable(object);
  }

  private <B extends GitSyncableEntity> Criteria createGitSyncCriteriaForRepoAndBranch(
      String repo, String branch, Boolean isFindDefaultFromOtherBranches, Class<B> entityClass) {
    if (repo == null || branch == null || repo.equals(DEFAULT) || branch.equals(DEFAULT)) {
      return getCriteriaWhenGitSyncNotEnabled(entityClass);
    }
    return getRepoAndBranchCriteria(repo, branch, isFindDefaultFromOtherBranches, entityClass);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> List<B> find(Criteria criteria, Pageable pageable,
      String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    final Criteria gitSyncCriteria =
        updateCriteriaIfGitSyncEnabled(projectIdentifier, orgIdentifier, accountId, entityClass);
    List<Criteria> criteriaList = Arrays.asList(criteria, gitSyncCriteria);
    Query query = new Query()
                      .addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])))
                      .with(pageable);
    return mongoTemplate.find(query, entityClass);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> boolean exists(
      Criteria criteria, String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    final Criteria gitSyncCriteria =
        updateCriteriaIfGitSyncEnabled(projectIdentifier, orgIdentifier, accountId, entityClass);
    List<Criteria> criteriaList = Arrays.asList(criteria, gitSyncCriteria);
    Query query =
        new Query().addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
    return mongoTemplate.exists(query, entityClass);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> B save(
      B objectToSave, ChangeType changeType, Class<B> entityClass) {
    final Supplier<Y> yamlFromEntity =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName()).getYamlFromEntity(objectToSave);
    return save(objectToSave, yamlFromEntity.get(), changeType, entityClass);
  }

  private boolean isGitSyncEnabled(String projectIdentifier, String orgIdentifier, String accountIdentifier) {
    return gitSyncSdkService.isGitSyncEnabled(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  private Criteria getCriteriaWhenGitSyncNotEnabled(Class entityClass) {
    final GitSdkEntityHandlerInterface gitSdkEntityHandlerInterface =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName());
    return new Criteria().andOperator(
        new Criteria().orOperator(Criteria.where(gitSdkEntityHandlerInterface.getIsFromDefaultBranchKey()).is(true),
            Criteria.where(gitSdkEntityHandlerInterface.getIsFromDefaultBranchKey()).exists(false)));
  }

  private Criteria getRepoAndBranchCriteria(
      String repo, String branch, Boolean isFindDefaultFromOtherBranches, Class entityClass) {
    final GitSdkEntityHandlerInterface gitSdkEntityHandlerInterface =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName());
    Criteria criteria = new Criteria()
                            .and(gitSdkEntityHandlerInterface.getBranchKey())
                            .is(branch)
                            .and(gitSdkEntityHandlerInterface.getYamlGitConfigRefKey())
                            .is(repo);
    if (isFindDefaultFromOtherBranches) {
      return new Criteria().orOperator(criteria,
          Criteria.where(gitSdkEntityHandlerInterface.getIsFromDefaultBranchKey())
              .is(true)
              .and(gitSdkEntityHandlerInterface.getYamlGitConfigRefKey())
              .ne(repo));
    }
    return criteria;
  }

  private Criteria updateCriteriaIfGitSyncEnabled(
      String projectIdentifier, String orgIdentifier, String accountId, Class entityClass) {
    if (isGitSyncEnabled(projectIdentifier, orgIdentifier, accountId)) {
      final GitEntityInfo gitBranchInfo = getGitEntityInfo();
      if (gitBranchInfo == null) {
        return createGitSyncCriteriaForRepoAndBranch(null, null, null, entityClass);
      }
      return createGitSyncCriteriaForRepoAndBranch(gitBranchInfo.getYamlGitConfigId(), gitBranchInfo.getBranch(),
          gitBranchInfo.isFindDefaultFromOtherBranches(), entityClass);
    }
    return new Criteria();
  }
}