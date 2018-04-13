package software.wings.service.impl.yaml;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.harness.data.structure.UUIDGenerator;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.util.FS;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GitConfig;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.core.ssh.executors.SshSessionConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

public class GitClientImplTest {
  public static final String oldObjectIdString = "0000000000000000000000000000000000000000";
  public static final String newObjectIdString = "1111111111111111111111111111111111111111";
  public static final String content = "this is mock yaml content";
  public static final String oldPath = "root/dir/file_old_path";
  public static final String newPath = "root/dir/file_new_path";

  // Git sync credentials (Keeping it here for now)
  private static final String GIT_REPO_URL = "git@github.com:rathn/Rathna-Test.git";
  private static final String PRIVATE_KEY = "/Users/rathna/.ssh/id_rsa";
  private static final String GIT_USER = "git";

  private static final Logger logger = LoggerFactory.getLogger(GitClientImplTest.class);

  private static final String localSSHKey = "-----BEGIN RSA PRIVATE KEY-----\n"
      + "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\n"
      + "-----END RSA PRIVATE KEY-----\n";

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

    MethodUtils.invokeMethod(gitClient, true, "addToGitDiffResult",
        new Object[] {Collections.singletonList(entry), diffResult, headCommitId, gitConfig, repository});
    assertEquals(1, diffResult.getGitFileChanges().size());
    GitFileChange gitFileChange = diffResult.getGitFileChanges().iterator().next();
    assertEquals(oldObjectIdString, gitFileChange.getObjectId());
    assertEquals(oldPath, gitFileChange.getFilePath());
    assertEquals(content, gitFileChange.getFileContent());

    diffResult.getGitFileChanges().clear();

    MethodUtils.invokeMethod(gitClient, true, "addToGitDiffResult",
        new Object[] {Collections.singletonList(entry), diffResult, headCommitId, gitConfig, repository});
    assertEquals(1, diffResult.getGitFileChanges().size());
    gitFileChange = diffResult.getGitFileChanges().iterator().next();
    assertEquals(newObjectIdString, gitFileChange.getObjectId());
    assertEquals(newPath, gitFileChange.getFilePath());
    assertEquals(content, gitFileChange.getFileContent());
  }

  private char[] getSSHKey(String path) {
    byte[] priKeyBytes = null;
    try {
      File filePrivateKey = new File(path);
      FileInputStream fis = new FileInputStream(path);
      priKeyBytes = new byte[(int) filePrivateKey.length()];
      fis.read(priKeyBytes);
      fis.close();
    } catch (IOException ioex) {
      ioex.printStackTrace();
    }

    StringBuilder sb = new StringBuilder();
    for (byte b : priKeyBytes) {
      sb.append(b);
    }

    return sb.toString().toCharArray();
  }

  public Git gitSyncCloneRepository() throws IOException, GitAPIException {
    SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
      @Override
      protected void configure(OpenSshConfig.Host host, Session session) {
        session.setConfig("StrictHostKeyChecking", "no");

        SshSessionConfig newConfig = aSshSessionConfig()
                                         .withKey(getSSHKey(PRIVATE_KEY))
                                         .withKeyName(UUIDGenerator.generateUuid())
                                         .withHost(host.getHostName())
                                         .withUserName(GIT_USER)
                                         .withPort(host.getPort())
                                         .build();

        try {
          session = getSSHSession(newConfig);
        } catch (JSchException jse) {
          logger.info("Could not get SSH session : " + jse.getMessage());
        }
      }

      // FROM:
      // https://stackoverflow.com/questions/13686643/using-keys-with-jgit-to-access-a-git-repository-securely/19931041#19931041
      @Override
      protected JSch getJSch(final OpenSshConfig.Host hc, FS fs) throws JSchException {
        JSch jsch = super.getJSch(hc, fs);
        jsch.removeAllIdentity();
        return jsch;
      }
    };

    Git result = null;
    File localPath = File.createTempFile("TestGitRepository", "");
    localPath.delete();
    result = Git.cloneRepository()
                 .setURI(GIT_REPO_URL)
                 .setDirectory(localPath)
                 .setTransportConfigCallback(transport -> {
                   SshTransport sshTransport = (SshTransport) transport;
                   sshTransport.setSshSessionFactory(sshSessionFactory);
                 })
                 .call();
    logger.info("Cloning repository to directory: " + result.getRepository().getDirectory());
    return result;
  }

  @Test
  public void testCloneRepoWithSSH() throws Exception {
    try {
      Git gitResult = gitSyncCloneRepository();
    } catch (GitAPIException gae) {
      logger.error("Git Clone with SSH failed for repository: " + gae.getMessage());
    }
  }
}
