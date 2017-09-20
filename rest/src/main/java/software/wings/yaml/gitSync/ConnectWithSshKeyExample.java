package software.wings.yaml.gitSync;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.yaml.gitSync.YamlGitSync.SyncMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import javax.inject.Inject;

public class ConnectWithSshKeyExample {
  @Inject YamlGitSyncService yamlGitSyncService;

  public static void main(String[] args) throws IOException, GitAPIException {
    String pathToSecrets = args[0];

    ConnectWithSshKeyExample cwske = new ConnectWithSshKeyExample();

    BufferedReader passphraseBR = new BufferedReader(new FileReader(pathToSecrets + "passphrase.txt"));
    BufferedReader sshKeyPathBR = new BufferedReader(new FileReader(pathToSecrets + "sshKeyPath.txt"));

    String passphrase = passphraseBR.readLine();
    String sshKeyPath = sshKeyPathBR.readLine();

    GitSyncHelper gsh = new GitSyncHelper(passphrase, sshKeyPath);

    //---------------------
    File localPath = File.createTempFile("TestGitRepository", "");
    localPath.delete();
    localPath.mkdirs();

    // prints absolute path
    System.out.println("Absolute path: " + localPath.getAbsolutePath());
    //---------------------

    Git git = gsh.clone("git@github.com:wings-software/yml-test.git", localPath);

    //---------------------
    // Create a new file and add it to the index
    String fileName = "test_file1.txt";

    File newFile = new File(localPath, fileName);
    newFile.createNewFile();
    FileWriter writer = new FileWriter(newFile);

    String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());

    writer.write("Test data"
        + "\n"
        + "test data2"
        + "\n"
        + "Last Commit: " + timestamp + "\n");
    writer.close();
    //---------------------

    DirCache dirCache = git.add().addFilepattern(fileName).call();

    RevCommit rev = gsh.commit("bsollish", "bob@harness.io", "My first test commit");

    System.out.println(rev.toString());

    Iterable<PushResult> pushResults = gsh.push("origin");
  }

  public YamlGitSync createYamlGitSync(String pathToSecrets) throws IOException, GitAPIException {
    String accountId = "kmpySmUISimoRrJL6NL73w";

    BufferedReader passphraseBR = new BufferedReader(new FileReader(pathToSecrets + "passphrase.txt"));
    BufferedReader sshKeyPathBR = new BufferedReader(new FileReader(pathToSecrets + "sshKeyPath.txt"));

    String passphrase = passphraseBR.readLine();
    String sshKeyPath = sshKeyPathBR.readLine();
    String sshKey = "blah";

    YamlGitSync ygs = YamlGitSync.Builder.aYamlGitSync()
                          .withAccountId(accountId)
                          .withEntityId(accountId)
                          .withEnabled(true)
                          .withUrl("git@github.com:wings-software/yml-test.git")
                          .withRootPath("/")
                          .withSshKey(sshKey)
                          .withPassphrase(passphrase)
                          .withSyncMode(SyncMode.HARNESS_TO_GIT)
                          .build();
    return yamlGitSyncService.save(accountId, ygs);
  }
}
