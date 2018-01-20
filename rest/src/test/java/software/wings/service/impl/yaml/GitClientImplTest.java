package software.wings.service.impl.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;
import software.wings.beans.GitConfig;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GitClientImplTest {
  public static final String oldObjectIdString = "0000000000000000000000000000000000000000";
  public static final String newObjectIdString = "1111111111111111111111111111111111111111";
  public static final String content = "this is mock yaml content";
  public static final String oldPath = "root/dir/file_old_path";
  public static final String newPath = "root/dir/file_new_path";

  @Test
  public void testAddToGitDiffResult() throws Exception {
    DiffEntry entry = mock(DiffEntry.class);
    GitClientImpl gitClient = spy(GitClientImpl.class);
    Repository repository = mock(Repository.class);

    AbbreviatedObjectId oldAbbreviatedObjectId = AbbreviatedObjectId.fromString(oldObjectIdString);
    AbbreviatedObjectId newAbbreviatedObjectId = AbbreviatedObjectId.fromString(newObjectIdString);

    when(entry.getOldPath()).thenReturn(oldPath);
    when(entry.getNewPath()).thenReturn(newPath);
    when(entry.getOldId()).thenReturn(oldAbbreviatedObjectId);
    when(entry.getNewId()).thenReturn(newAbbreviatedObjectId);
    when(entry.getChangeType()).thenReturn(DiffEntry.ChangeType.DELETE).thenReturn(ChangeType.ADD);

    byte[] bytes = content.getBytes(Charset.forName("UTF-8"));
    ObjectLoader loader = new ObjectLoader.SmallObject(0, bytes);
    when(repository.open(any())).thenReturn(loader);

    GitConfig gitConfig = GitConfig.builder().accountId("000111222333").build();
    ObjectId headCommitId = ObjectId.fromString("2222222222222222222222222222222222222222");

    GitDiffResult diffResult = GitDiffResult.builder()
                                   .branch(gitConfig.getBranch())
                                   .repoName(gitConfig.getRepoUrl())
                                   .gitFileChanges(new ArrayList<>())
                                   .build();

    invokePrivateMethod(Collections.singletonList(entry), diffResult, headCommitId, gitConfig, repository,
        "addToGitDiffResult", gitClient);
    assertEquals(1, diffResult.getGitFileChanges().size());
    GitFileChange gitFileChange = diffResult.getGitFileChanges().iterator().next();
    assertEquals(oldObjectIdString, gitFileChange.getObjectId());
    assertEquals(oldPath, gitFileChange.getFilePath());
    assertEquals(content, gitFileChange.getFileContent());

    diffResult.getGitFileChanges().clear();

    invokePrivateMethod(Collections.singletonList(entry), diffResult, headCommitId, gitConfig, repository,
        "addToGitDiffResult", gitClient);
    assertEquals(1, diffResult.getGitFileChanges().size());
    gitFileChange = diffResult.getGitFileChanges().iterator().next();
    assertEquals(newObjectIdString, gitFileChange.getObjectId());
    assertEquals(newPath, gitFileChange.getFilePath());
    assertEquals(content, gitFileChange.getFileContent());
  }

  private void invokePrivateMethod(List<DiffEntry> diffs, GitDiffResult diffResult, ObjectId headCommitId,
      GitConfig gitConfig, Repository repository, String methodName, GitClientImpl gitClient) {
    java.lang.reflect.Method method = null;
    try {
      method = GitClientImpl.class.getDeclaredMethod(
          methodName, new Class[] {List.class, GitDiffResult.class, ObjectId.class, GitConfig.class, Repository.class});
      method.setAccessible(true);
    } catch (Exception e) {
      assertTrue(false);
    }

    try {
      method.invoke(gitClient, new Object[] {diffs, diffResult, headCommitId, gitConfig, repository});
      method.setAccessible(false);
    } catch (Exception e) {
    }
  }
}
