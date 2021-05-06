package io.harness.ng.core.entitysetupusage;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.manage.GlobalContextManager;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage.EntitySetupUsageKeys;

import com.google.inject.Singleton;
import java.util.List;
import java.util.Objects;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@OwnedBy(DX)
public class EntitySetupUsageQueryFilterHelper {
  public Criteria createCriteriaFromEntityFilter(
      String accountIdentifier, String referredEntityFQN, EntityType referredEntityType, String searchTerm) {
    Criteria criteria = new Criteria();
    criteria.and(EntitySetupUsageKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(EntitySetupUsageKeys.referredEntityFQN).is(referredEntityFQN);
    if (referredEntityType != null) {
      criteria.and(EntitySetupUsageKeys.referredEntityType).is(referredEntityType.getYamlName());
    }
    if (isNotBlank(searchTerm)) {
      criteria.orOperator(Criteria.where(EntitySetupUsageKeys.referredEntityName).regex(searchTerm),
          Criteria.where(EntitySetupUsageKeys.referredByEntityName).regex(searchTerm));
    }
    populateGitCriteriaForReferredEntity(criteria);
    return criteria;
  }

  private Criteria createCriteriaForDefaultReferredEntity() {
    return new Criteria().orOperator(Criteria.where(EntitySetupUsageKeys.referredEntityIsDefault).is(true),
        Criteria.where(EntitySetupUsageKeys.referredEntityIsDefault).exists(false));
  }

  private Criteria createCriteriaForDefaultReferredByEntity() {
    return new Criteria().orOperator(Criteria.where(EntitySetupUsageKeys.referredByEntityIsDefault).is(true),
        Criteria.where(EntitySetupUsageKeys.referredByEntityIsDefault).exists(false));
  }

  private boolean gitContextIsPresent(EntityGitDetails entityGitDetails) {
    if (entityGitDetails == null
        || entityGitDetails.getBranch() == null && entityGitDetails.getRepoIdentifier() == null) {
      return false;
    }
    return true;
  }

  public Criteria createCriteriaForListAllReferredUsages(
      String accountIdentifier, String referredByEntityFQN, EntityType referredEntityType, String searchTerm) {
    Criteria criteria = new Criteria();
    criteria.and(EntitySetupUsageKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(EntitySetupUsageKeys.referredByEntityFQN).is(referredByEntityFQN);
    if (referredEntityType != null) {
      criteria.and(EntitySetupUsageKeys.referredEntityType).is(referredEntityType.getYamlName());
    }
    if (isNotBlank(searchTerm)) {
      criteria.orOperator(Criteria.where(EntitySetupUsageKeys.referredEntityName).regex(searchTerm),
          Criteria.where(EntitySetupUsageKeys.referredByEntityName).regex(searchTerm));
    }
    populateGitCriteriaForReferredByEntity(criteria);
    return criteria;
  }

  public Criteria createCriteriaForListAllReferredUsagesBatch(String accountIdentifier,
      List<String> referredByEntityFQNList, EntityType referredByEntityType, EntityType referredEntityType) {
    return Criteria.where(EntitySetupUsageKeys.accountIdentifier)
        .is(accountIdentifier)
        .and(EntitySetupUsageKeys.referredByEntityFQN)
        .in(referredByEntityFQNList)
        .and(EntitySetupUsageKeys.referredByEntityType)
        .is(referredByEntityType.getYamlName())
        .and(EntitySetupUsageKeys.referredEntityType)
        .is(referredEntityType.getYamlName());
  }

  public Criteria createCriteriaToCheckWhetherThisEntityIsReferred(
      String accountIdentifier, String referredEntityFQN, EntityType referredEntityType) {
    Criteria criteria = new Criteria();
    criteria.and(EntitySetupUsageKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(EntitySetupUsageKeys.referredEntityFQN).is(referredEntityFQN);
    if (referredEntityType != null) {
      criteria.and(EntitySetupUsageKeys.referredEntityType).is(referredEntityType.getYamlName());
    }
    populateGitCriteriaForReferredEntity(criteria);
    return criteria;
  }

  private void populateGitCriteriaForReferredEntity(Criteria criteria) {
    EntityGitDetails entityGitDetails = getGitDetailsFromThreadContext();
    if (gitContextIsPresent(entityGitDetails)) {
      criteria.and(EntitySetupUsageKeys.referredEntityRepoIdentifier).is(entityGitDetails.getRepoIdentifier());
      criteria.and(EntitySetupUsageKeys.referredEntityBranch).is(entityGitDetails.getBranch());
    } else {
      Criteria criteriaToGetDefaultEntity = createCriteriaForDefaultReferredEntity();
      criteria.andOperator(criteriaToGetDefaultEntity);
    }
  }

  private EntityGitDetails getGitDetailsFromThreadContext() {
    GlobalContextData globalContextData = GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);
    if (globalContextData == null) {
      return null;
    }
    final GitEntityInfo gitBranchInfo =
        ((GitSyncBranchContext) Objects.requireNonNull(globalContextData)).getGitBranchInfo();
    if (gitBranchInfo != null && !isNullGitContext(gitBranchInfo)) {
      return EntityGitDetails.builder()
          .branch(gitBranchInfo.getBranch())
          .repoIdentifier(gitBranchInfo.getYamlGitConfigId())
          .build();
    }
    return null;
  }

  private boolean isNullGitContext(GitEntityInfo gitBranchInfo) {
    // todo @Abhinav Maybe we should use null in place of default
    final String DEFAULT = "__default__";
    boolean isRepoNull =
        isEmpty(gitBranchInfo.getYamlGitConfigId()) || gitBranchInfo.getYamlGitConfigId().equals(DEFAULT);
    boolean isBranchNull = isEmpty(gitBranchInfo.getBranch()) || gitBranchInfo.getBranch().equals(DEFAULT);
    if (!isRepoNull && isBranchNull || isRepoNull && !isBranchNull) {
      throw new InvalidRequestException(
          String.format("The repo should be provided with the branch, the request has repo %s, branch %s",
              gitBranchInfo.getYamlGitConfigId(), gitBranchInfo.getBranch()));
    }
    return isRepoNull && isBranchNull;
  }

  private void populateGitCriteriaForReferredByEntity(Criteria criteria) {
    EntityGitDetails entityGitDetails = getGitDetailsFromThreadContext();
    if (gitContextIsPresent(entityGitDetails)) {
      criteria.and(EntitySetupUsageKeys.referredByEntityRepoIdentifier).is(entityGitDetails.getRepoIdentifier());
      criteria.and(EntitySetupUsageKeys.referredByEntityBranch).is(entityGitDetails.getBranch());
    } else {
      Criteria criteriaToGetDefaultEntity = createCriteriaForDefaultReferredByEntity();
      criteria.andOperator(criteriaToGetDefaultEntity);
    }
  }
}
