package io.harness.gitsync.core.impl;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.git.model.ChangeType;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import io.harness.repositories.repositories.yamlGitConfig.YamlGitConfigRepository;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.service.impl.trigger.WebhookEventUtils;

import com.google.inject.Inject;
import java.util.Collections;
import javax.ws.rs.core.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class YamlGitServiceImplTest extends CategoryTest {
  public static final String ACCOUNTID = "ACCOUNTID";
  static final String GH_PUSH_REQ_FILE =
      "136-git-sync-manager/src/test/resources/software/wings/service/impl/webhook/github_push_request.json";

  @Inject YamlGitConfigRepository yamlGitConfigRepository;
  @Mock YamlGitConfigService yamlGitConfigService;
  @Mock SecretManagerClientService ngSecretService;
  @Mock GitSyncErrorService gitSyncErrorService;
  @Mock WaitNotifyEngine waitNotifyEngine;
  @Mock private HttpHeaders httpHeaders;
  @Mock YamlChangeSetService yamlChangeSetService;
  @Mock GitCommitService gitCommitService;
  @Mock WebhookEventUtils webhookEventUtils;

  @InjectMocks @Inject @Spy public YamlGitServiceImpl yamlGitService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
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
    yamlGitService.removeGitSyncErrors(ACCOUNTID, null, null, Collections.singletonList(gitFileChange));
    verify(gitSyncErrorService, times(1))
        .deleteByAccountIdOrgIdProjectIdAndFilePath(ACCOUNTID, null, null, Collections.singletonList("path"));
  }
}
