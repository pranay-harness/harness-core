package software.wings.yaml.gitSync.reference;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.util.FS;

import java.io.File;
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
      Git result = cloneCommand.call();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
