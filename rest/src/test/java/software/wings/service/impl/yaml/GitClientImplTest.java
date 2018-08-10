package software.wings.service.impl.yaml;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;

import com.google.inject.Inject;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.harness.data.structure.UUIDGenerator;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.core.ssh.executors.SshSessionConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

public class GitClientImplTest extends WingsBaseTest {
  public static final String oldObjectIdString = "0000000000000000000000000000000000000000";
  public static final String newObjectIdString = "1111111111111111111111111111111111111111";
  public static final String content = "this is mock yaml content";
  public static final String oldPath = "root/dir/file_old_path";
  public static final String newPath = "root/dir/file_new_path";

  // Git sync credentials (Keeping it here for now)
  private static final String GIT_REPO_URL = "git@github.com:wings-software/yaml-test.git";
  // private static final String PRIVATE_KEY = "/Users/rathna/.ssh/id_rsa";
  private static final String GIT_USER = "git";

  private static final Logger logger = LoggerFactory.getLogger(GitClientImplTest.class);

  @Mock private GitClientHelper gitClientHelper;
  @Inject @InjectMocks private GitClientImpl gitClient;
  private static final String localSSHKey_Rathna = "-----BEGIN RSA PRIVATE KEY-----\n"
      + "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\n"
      + "-----END RSA PRIVATE KEY-----\n";

  private static final String localSSHKey_Anubhaw = "-----BEGIN RSA PRIVATE KEY-----\n"
      + "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\n"
      + "-----END RSA PRIVATE KEY-----\n";

  @Test
  public void testAddToGitDiffResult() throws Exception {
    DiffEntry entry = mock(DiffEntry.class);
    Repository repository = mock(Repository.class);

    AbbreviatedObjectId oldAbbreviatedObjectId = AbbreviatedObjectId.fromString(oldObjectIdString);
    AbbreviatedObjectId newAbbreviatedObjectId = AbbreviatedObjectId.fromString(newObjectIdString);

    when(entry.getOldPath()).thenReturn(oldPath);
    when(entry.getNewPath()).thenReturn(newPath);
    when(entry.getOldId()).thenReturn(oldAbbreviatedObjectId);
    when(entry.getNewId()).thenReturn(newAbbreviatedObjectId);
    when(entry.getChangeType()).thenReturn(DiffEntry.ChangeType.DELETE).thenReturn(DiffEntry.ChangeType.ADD);
    when(gitClientHelper.getChangeType(any())).thenReturn(ChangeType.DELETE).thenReturn(ChangeType.ADD);

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
    } catch (IOException ex) {
      logger.error("", ex);
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
                                         .withKey(localSSHKey_Anubhaw.toCharArray())
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
    String PATH = "/Users/rathna/tmp";
    // localPath.delete();
    result = Git.cloneRepository()
                 .setURI(GIT_REPO_URL)
                 .setDirectory(new File(PATH))
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
    } catch (Exception gae) {
      logger.error("Git Clone with SSH failed for repository: " + gae.getMessage());
    }
  }
}
