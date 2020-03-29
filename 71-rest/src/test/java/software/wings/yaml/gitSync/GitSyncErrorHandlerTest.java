package software.wings.yaml.gitSync;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.yaml.sync.GitSyncService;
import software.wings.yaml.errorhandling.GitSyncError;

import java.util.ArrayList;
import java.util.List;

public class GitSyncErrorHandlerTest extends WingsBaseTest {
  @Mock GitSyncService gitSyncService;
  @InjectMocks @Inject GitSyncErrorHandler gitSyncErrorHandler;

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testHandle() {
    List<GitSyncError> response = new ArrayList<>();
    GitSyncError expiredGitSyncError = GitSyncError.builder().gitCommitId("commitId").fullSyncPath(false).build();
    response.add(expiredGitSyncError);
    PageResponse<GitSyncError> pageResponse = aPageResponse().withResponse(response).build();
    when(gitSyncService.fetchErrors(any())).thenReturn(pageResponse);
    gitSyncErrorHandler.handle(getAccount("PAID"));
    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(gitSyncService, times(1)).deleteGitSyncErrorAndLogFileActivity(argumentCaptor.capture(), any(), any());
    assert argumentCaptor.getValue().size() == 1;
  }
}