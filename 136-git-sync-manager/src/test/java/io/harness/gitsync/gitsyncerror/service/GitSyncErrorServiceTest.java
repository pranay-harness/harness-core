package io.harness.gitsync.gitsyncerror.service;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.gitsyncerror.dao.api.repositories.gitSyncError.GitSyncErrorRepository;
import io.harness.gitsync.gitsyncerror.impl.GitSyncErrorServiceImpl;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

public class GitSyncErrorServiceTest extends CategoryTest {
  @Mock GitSyncErrorRepository gitSyncErrorRepository;
  @InjectMocks @Inject GitSyncErrorServiceImpl gitSyncErrorService;
  public static final String ACCOUNT_ID = "ACCOUNTID";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testDeleteByAccountIdOrgIdProjectIdAndFilePath() {
    gitSyncErrorService.deleteByAccountIdOrgIdProjectIdAndFilePath(
        ACCOUNT_ID, null, null, Collections.singletonList("path"));
    verify(gitSyncErrorRepository, times(1))
        .removeByAccountIdAndOrganizationIdAndProjectIdAndYamlFilePathIn(
            ACCOUNT_ID, null, null, Collections.singletonList("path"));
  }
}
