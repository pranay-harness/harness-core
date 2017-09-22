package software.wings.yaml.gitSync.devexample;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import software.wings.yaml.gitSync.GitSyncHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class ConnectWithSshKeyExample {
  public static void main(String[] args) throws IOException, GitAPIException {
    String pathToSecrets = args[0];

    BufferedReader passphraseBR = new BufferedReader(new FileReader(pathToSecrets + "passphrase.txt"));
    BufferedReader sshKeyPathBR = new BufferedReader(new FileReader(pathToSecrets + "sshKeyPath.txt"));

    String passphrase = passphraseBR.readLine();
    String sshKeyPath = sshKeyPathBR.readLine();

    sshKeyPath = "/Users/bsollish/.ssh/id_rsa";

    GitSyncHelper gsh = new GitSyncHelper(passphrase, sshKeyPath);

    //---------------------
    File repoPath = File.createTempFile("TestGitRepository", "");
    repoPath.delete();
    repoPath.mkdirs();

    // prints absolute path
    System.out.println("Absolute path: " + repoPath.getAbsolutePath());
    //---------------------

    Git git = gsh.clone("git@github.com:wings-software/yml-test.git", repoPath);

    //---------------------
    // Create a new file and add it to the index
    String fileName = "test_file1.txt";

    File newFile = new File(repoPath, fileName);
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

    // need to clean up TEMP files
    repoPath.delete();
  }
}