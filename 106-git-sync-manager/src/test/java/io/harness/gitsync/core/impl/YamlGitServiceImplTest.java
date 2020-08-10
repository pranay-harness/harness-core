package io.harness.gitsync.core.impl;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.ManagerDelegateServiceDriver;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.EntityScope;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.common.beans.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.dao.api.repositories.yamlGitConfig.YamlGitConfigRepository;
import io.harness.gitsync.common.dao.api.repositories.yamlGitFolderConfig.YamlGitFolderConfigRepository;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import io.harness.ng.core.gitsync.ChangeType;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.waiter.WaitNotifyEngine;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Collections;
import java.util.Optional;

public class YamlGitServiceImplTest extends CategoryTest {
  public static final String ACCOUNTID = "ACCOUNTID";
  @Inject YamlGitConfigRepository yamlGitConfigRepository;
  @Inject YamlGitFolderConfigRepository yamlGitFolderConfigRepository;
  @Mock YamlGitConfigService yamlGitConfigService;
  @Mock SecretManagerClientService ngSecretService;
  @Mock ManagerDelegateServiceDriver managerDelegateServiceDriver;
  @Mock GitSyncErrorService gitSyncErrorService;
  @Mock WaitNotifyEngine waitNotifyEngine;
  @InjectMocks @Inject @Spy public YamlGitServiceImpl yamlGitService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testHandleHarnessChangeSet() {
    GitFileChange gitFileChange = GitFileChange.builder()
                                      .rootPath("abc")
                                      .changeType(ChangeType.ADD)
                                      .fileContent("abc")
                                      .filePath("path")
                                      .syncFromGit(false)
                                      .build();
    YamlChangeSet yamlChangeSet = YamlChangeSet.builder()
                                      .status(YamlChangeSet.Status.QUEUED)
                                      .scope(EntityScope.Scope.ACCOUNT)
                                      .accountId(ACCOUNTID)
                                      .gitToHarness(false)
                                      .gitFileChanges(Collections.singletonList(gitFileChange))
                                      .build();

    doReturn(null).when(ngSecretService).getEncryptionDetails(any());
    doReturn(YamlGitConfigDTO.builder().build())
        .when(yamlGitConfigService)
        .getByFolderIdentifierAndIsEnabled(null, null, ACCOUNTID, null);
    doReturn(Optional.of(GitConfigDTO.builder().build()))
        .when(yamlGitConfigService)
        .getGitConfig(any(), any(), any(), any());
    doReturn("1234").when(managerDelegateServiceDriver).sendTaskAsync(any(), any(), any());
    yamlGitService.handleHarnessChangeSet(yamlChangeSet, ACCOUNTID);
    verify(waitNotifyEngine, times(1)).waitForAllOn(any(), any(), any());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testHandleHarnessChangeSetError() {
    GitFileChange gitFileChange = GitFileChange.builder()
                                      .rootPath("abc")
                                      .changeType(ChangeType.ADD)
                                      .fileContent("abc")
                                      .filePath("path")
                                      .syncFromGit(false)
                                      .build();
    YamlChangeSet yamlChangeSet = YamlChangeSet.builder()
                                      .status(YamlChangeSet.Status.QUEUED)
                                      .scope(EntityScope.Scope.ACCOUNT)
                                      .accountId(ACCOUNTID)
                                      .gitToHarness(false)
                                      .gitFileChanges(Collections.singletonList(gitFileChange))
                                      .build();

    assertThatThrownBy(() -> yamlGitService.handleHarnessChangeSet(yamlChangeSet, ACCOUNTID));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testRemoveGitSyncError() {
    GitFileChange gitFileChange = GitFileChange.builder()
                                      .rootPath("abc")
                                      .changeType(ChangeType.ADD)
                                      .fileContent("abc")
                                      .filePath("path")
                                      .syncFromGit(false)
                                      .build();
    yamlGitService.removeGitSyncErrors(ACCOUNTID, null, null, Collections.singletonList(gitFileChange), false);
    verify(gitSyncErrorService, times(1))
        .deleteByAccountIdOrgIdProjectIdAndFilePath(ACCOUNTID, null, null, Collections.singletonList("path"));
  }
}