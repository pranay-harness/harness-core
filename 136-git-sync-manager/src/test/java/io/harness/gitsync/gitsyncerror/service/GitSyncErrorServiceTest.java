package io.harness.gitsync.gitsyncerror.service;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.gitsyncerror.impl.GitSyncErrorServiceImpl;
import io.harness.repositories.gitSyncError.GitSyncErrorRepository;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.DX)
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
        .deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndCompleteFilePathIn(
            ACCOUNT_ID, null, null, Collections.singletonList("path"));
  }
}
