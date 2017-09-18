package software.wings.yaml.gitSync.reference;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ConnectWithSshKey2 {
  public static void main(String[] args) throws IOException, GitAPIException {
    SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
      @Override
      protected void configure(Host host, Session session) {
        // do nothing
      }

      @Override
      protected JSch createDefaultJSch(FS fs) throws JSchException {
        String passphrase = "fish monkey dog mouse";

        JSch defaultJSch = super.createDefaultJSch(fs);
        // defaultJSch.addIdentity( "~/.ssh/bobwings-GitHub" );
        // defaultJSch.addIdentity( "/Users/bsollish/.ssh/bobwings-GitHub", passphrase);
        defaultJSch.addIdentity("/Users/bsollish/.ssh/id_rsa", passphrase);
        return defaultJSch;
      }
    };

    File localPath = File.createTempFile("TestGitRepository", "");
    // File localPath = File.createTempFile("yaml", "");
    localPath.delete();
    localPath.mkdirs();

    CloneCommand cloneCommand = Git.cloneRepository();
    // cloneCommand.setURI( "ssh://user@example.com/repo.git" );
    cloneCommand.setURI("git@github.com:wings-software/yml-test.git");
    cloneCommand.setDirectory(localPath);
    cloneCommand.setTransportConfigCallback(new TransportConfigCallback() {
      @Override
      public void configure(Transport transport) {
        SshTransport sshTransport = (SshTransport) transport;
        sshTransport.setSshSessionFactory(sshSessionFactory);
      }
    });

    try {
      Git git = cloneCommand.call();

      // Create a new file and add it to the index
      String fileName = "test_file1.txt";

      File newFile = new File(localPath, fileName);
      newFile.createNewFile();
      FileWriter writer = new FileWriter(newFile);
      writer.write("Test data"
          + "\n"
          + "test data2"
          + "\n");
      writer.close();
      git.add().addFilepattern(fileName).call();

      spitOutStatus(git);

      // Now, we do the commit with a message
      RevCommit rev = git.commit().setAuthor("bsollish", "bob@harness.io").setMessage("My first test commit").call();

      System.out.println(rev.toString());

      // Now we push the commit
      // Iterable<PushResult> pushResults = git.push().setPushAll().call();

      PushCommand pushCommand = git.push();
      pushCommand.setRemote("origin");
      // pushCommand.setRefSpecs( new RefSpec( "release_2_0_2:release_2_0_2" ) );

      pushCommand.setTransportConfigCallback(new TransportConfigCallback() {
        @Override
        public void configure(Transport transport) {
          SshTransport sshTransport = (SshTransport) transport;
          sshTransport.setSshSessionFactory(sshSessionFactory);
        }
      });

      Iterable<PushResult> pushResults = pushCommand.call();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void spitOutStatus(Git git) {
    try {
      Status status = git.status().call();

      System.out.println("Added: " + status.getAdded());
      System.out.println("Changed: " + status.getChanged());
      System.out.println("Conflicting: " + status.getConflicting());
      System.out.println("ConflictingStageState: " + status.getConflictingStageState());
      System.out.println("IgnoredNotInIndex: " + status.getIgnoredNotInIndex());
      System.out.println("Missing: " + status.getMissing());
      System.out.println("Modified: " + status.getModified());
      System.out.println("Removed: " + status.getRemoved());
      System.out.println("Untracked: " + status.getUntracked());
      System.out.println("UntrackedFolders: " + status.getUntrackedFolders());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
