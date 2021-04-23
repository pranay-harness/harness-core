package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.GitSdkTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SampleBean;
import io.harness.category.element.UnitTests;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.SampleBeanEntityGitPersistenceHelperServiceImpl;
import io.harness.gitsync.branching.GitBranchingHelper;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchThreadLocal;
import io.harness.gitsync.persistance.GitSyncableEntity.GitSyncableEntityKeys;
import io.harness.gitsync.scm.EntityObjectIdUtils;
import io.harness.gitsync.scm.SCMGitSyncHelper;
import io.harness.gitsync.scm.beans.ScmCreateFileResponse;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(DX)
public class GitAwarePersistenceImplTest extends GitSdkTestBase {
  @Mock EntityKeySource entityKeySource;
  @Mock Map<String, GitSdkEntityHandlerInterface> gitPersistenceHelperServiceMap;
  @Mock SCMGitSyncHelper scmGitSyncHelper;
  @Mock GitSyncMsvcHelper gitSyncMsvcHelper;
  @Inject MongoTemplate mongoTemplate;
  GitAwarePersistenceImpl gitAwarePersistence;
  final String projectIdentifier = "proj";
  final String orgIdentifier = "org";
  final String accountIdentifier = "acc";
  final String identifier = "id";
  final String identifier_1 = "id1";

  final SampleBean sampleBean = SampleBean.builder()
                                    .test1("test")
                                    .projectIdentifier(projectIdentifier)
                                    .orgIdentifier(orgIdentifier)
                                    .accountIdentifier(accountIdentifier)
                                    .identifier(identifier)
                                    .build();

  final SampleBean sampleBean1 = SampleBean.builder()
                                     .test1("test1")
                                     .projectIdentifier(projectIdentifier)
                                     .orgIdentifier(orgIdentifier)
                                     .accountIdentifier(accountIdentifier)
                                     .identifier(identifier_1)
                                     .build();

  @Before
  public void setup() {
    initMocks(this);
    gitAwarePersistence = new GitAwarePersistenceImpl(mongoTemplate, entityKeySource,
        new GitBranchingHelper(mongoTemplate), gitPersistenceHelperServiceMap, scmGitSyncHelper, gitSyncMsvcHelper);
    doNothing().when(gitSyncMsvcHelper).postPushInformationToGitMsvc(any(), any(), any());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSave_0() {
    doSave(sampleBean, false, "branch");
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testFind() {
    final GitEntityInfo newBranch = GitEntityInfo.builder().branch("branch").yamlGitConfigId("ygs").build();
    try (GitSyncBranchThreadLocal.Guard guard = GitSyncBranchThreadLocal.gitBranchGuard(newBranch)) {
      final SampleBean sampleBean_saved = doSave(sampleBean, false, "branch");
      final SampleBean sampleBean_saved_1 = doSave(sampleBean1, false, "branch");
      assertFindReturnsObject(sampleBean_saved);
      assertFindReturnsObject(sampleBean_saved_1);
    }
  }

  private void assertFindReturnsObject(SampleBean sampleBean_saved) {
    final Criteria criteria = Criteria.where(GitSyncableEntityKeys.id).is(sampleBean_saved.getId());
    final Optional<SampleBean> retObject =
        gitAwarePersistence.findOne(criteria, projectIdentifier, orgIdentifier, accountIdentifier, SampleBean.class);
    assertThat(retObject.isPresent()).isTrue();
  }

  private SampleBean doSave(SampleBean sampleBean, boolean isDefaultBranch, String branch) {
    doReturn(new SampleBeanEntityGitPersistenceHelperServiceImpl())
        .when(gitPersistenceHelperServiceMap)
        .get(anyString());
    final String objectIdOfYaml = EntityObjectIdUtils.getObjectIdOfYaml(sampleBean);
    doReturn(ScmCreateFileResponse.builder()
                 .accountIdentifier("acc")
                 .objectId(objectIdOfYaml)
                 .yamlGitConfigId("ygs")
                 .branch(branch)
                 .pushToDefaultBranch(isDefaultBranch)
                 .build())
        .when(scmGitSyncHelper)
        .pushToGit(any(), anyString(), any(), any());
    doReturn(true).when(entityKeySource).fetchKey(any());
    return gitAwarePersistence.save(sampleBean, sampleBean, ChangeType.ADD, SampleBean.class);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSave_1() {
    final GitEntityInfo newBranch = GitEntityInfo.builder().branch("branch").yamlGitConfigId("ygs").build();
    try (GitSyncBranchThreadLocal.Guard guard = GitSyncBranchThreadLocal.gitBranchGuard(newBranch)) {
      final SampleBean sampleBean_saved = doSave(sampleBean, false, "branch");
    }
    final GitEntityInfo branch_1 = GitEntityInfo.builder().branch("master").yamlGitConfigId("ygs").build();
    try (GitSyncBranchThreadLocal.Guard guard = GitSyncBranchThreadLocal.gitBranchGuard(newBranch)) {
      SampleBean sampleBean_1 = SampleBean.builder()
                                    .test1("test")
                                    .projectIdentifier(projectIdentifier)
                                    .orgIdentifier(orgIdentifier)
                                    .accountIdentifier(accountIdentifier)
                                    .identifier(identifier)
                                    .build();
      final SampleBean sampleBean_saved = doSave(sampleBean_1, true, "master");
    }

    final long count = mongoTemplate.count(query(new Criteria()), SampleBean.class);
    assertThat(count).isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSave_2() {
    final GitEntityInfo newBranch = GitEntityInfo.builder().branch("branch").yamlGitConfigId("ygs").build();
    try (GitSyncBranchThreadLocal.Guard guard = GitSyncBranchThreadLocal.gitBranchGuard(newBranch)) {
      final SampleBean sampleBean_saved = doSave(sampleBean, false, "branch");
    }
    final GitEntityInfo branch_1 = GitEntityInfo.builder().branch("master").yamlGitConfigId("ygs").build();
    try (GitSyncBranchThreadLocal.Guard guard = GitSyncBranchThreadLocal.gitBranchGuard(newBranch)) {
      SampleBean sampleBean_1 = SampleBean.builder()
                                    .test1("test1")
                                    .projectIdentifier(projectIdentifier)
                                    .orgIdentifier(orgIdentifier)
                                    .accountIdentifier(accountIdentifier)
                                    .identifier(identifier)
                                    .build();
      final SampleBean sampleBean_saved = doSave(sampleBean_1, true, "master");
    }

    final long count = mongoTemplate.count(query(new Criteria()), SampleBean.class);
    assertThat(count).isEqualTo(2);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSave_3() {
    final GitEntityInfo newBranch = GitEntityInfo.builder().branch("branch").yamlGitConfigId("ygs").build();
    try (GitSyncBranchThreadLocal.Guard guard = GitSyncBranchThreadLocal.gitBranchGuard(newBranch)) {
      SampleBean sampleBean_saved = doSave(sampleBean, true, "branch");
      assertFindReturnsObject(sampleBean_saved);
    }
    final GitEntityInfo branch_1 = GitEntityInfo.builder().branch("master").yamlGitConfigId("ygs").build();
    try (GitSyncBranchThreadLocal.Guard guard = GitSyncBranchThreadLocal.gitBranchGuard(branch_1)) {
      SampleBean sampleBean_1 = SampleBean.builder()
                                    .test1("test1")
                                    .projectIdentifier(projectIdentifier)
                                    .orgIdentifier(orgIdentifier)
                                    .accountIdentifier(accountIdentifier)
                                    .identifier(identifier)
                                    .build();
      SampleBean sampleBean_saved_1 = doSave(sampleBean_1, false, "master");
      assertFindReturnsObject(sampleBean_saved_1);
    }

    final long count = mongoTemplate.count(query(new Criteria()), SampleBean.class);
    assertThat(count).isEqualTo(2);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSave_4() {
    SampleBean sampleBean_saved;
    final GitEntityInfo newBranch = GitEntityInfo.builder().branch("branch").yamlGitConfigId("ygs").build();
    try (GitSyncBranchThreadLocal.Guard guard = GitSyncBranchThreadLocal.gitBranchGuard(newBranch)) {
      sampleBean_saved = doSave(sampleBean, true, "branch");
      assertFindReturnsObject(sampleBean_saved);
    }
    final GitEntityInfo branch_1 = GitEntityInfo.builder().branch("master").yamlGitConfigId("ygs").build();
    SampleBean sampleBean_saved_1;
    try (GitSyncBranchThreadLocal.Guard guard = GitSyncBranchThreadLocal.gitBranchGuard(newBranch)) {
      SampleBean sampleBean_1 = SampleBean.builder()
                                    .test1("test")
                                    .projectIdentifier(projectIdentifier)
                                    .orgIdentifier(orgIdentifier)
                                    .accountIdentifier(accountIdentifier)
                                    .identifier(identifier)
                                    .build();
      sampleBean_saved_1 = doSave(sampleBean_1, false, "master");
      assertFindReturnsObject(sampleBean_saved_1);
    }

    final long count = mongoTemplate.count(query(new Criteria()), SampleBean.class);
    assertThat(count).isEqualTo(1);
  }
}