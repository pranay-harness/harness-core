package software.wings.yaml.gitSync.devexample;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.yaml.gitSync.GitSyncHelper;
import software.wings.yaml.gitSync.YamlGitSync;
import software.wings.yaml.gitSync.YamlGitSync.SyncMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

public class ConnectWithSshKeyExample2 {
  public static void main(String[] args) throws IOException, GitAPIException {
    String pathToSecrets = args[0];

    //--------------------
    Injector injector = Guice.createInjector(new ConnectWithSshKeyModule());
    YamlGitSyncService yamlGitSyncService = injector.getInstance(YamlGitSyncService.class);
    //--------------------

    ConnectWithSshKeyExample2 cwske = new ConnectWithSshKeyExample2();

    YamlGitSync ygs = cwske.createYamlGitSync(yamlGitSyncService, pathToSecrets);

    GitSyncHelper gsh = new GitSyncHelper(ygs.getPassphrase(), ygs.getSshKey());

    //---------------------
    File localPath = File.createTempFile("synced-repos/" + ygs.getAccountId(), "");
    localPath.delete();
    localPath.mkdirs();

    // prints absolute path
    System.out.println("Absolute path: " + localPath.getAbsolutePath());
    //---------------------

    Git git = gsh.clone(ygs.getUrl(), localPath);

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

  public YamlGitSync createYamlGitSync(YamlGitSyncService yamlGitSyncService, String pathToSecrets)
      throws IOException, GitAPIException {
    String accountId = "kmpySmUISimoRrJL6NL73w";
    String appId = "";

    BufferedReader passphraseBR = new BufferedReader(new FileReader(pathToSecrets + "passphrase.txt"));
    BufferedReader sshKeyPathBR = new BufferedReader(new FileReader(pathToSecrets + "sshKeyPath.txt"));

    String passphrase = passphraseBR.readLine();
    String sshKeyPath = sshKeyPathBR.readLine();

    String sshKey = new Scanner(new File(sshKeyPath)).useDelimiter("\\Z").next();

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

    return yamlGitSyncService.save(accountId, appId, ygs);
  }
}
