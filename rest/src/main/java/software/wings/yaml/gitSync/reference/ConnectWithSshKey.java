package software.wings.yaml.gitSync.reference;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class ConnectWithSshKey {
  public static void main(String[] args) throws IOException, GitAPIException {
    ConnectWithSshKey obj = new ConnectWithSshKey();
    System.out.println(obj.getFile("yaml/config_example1.yaml"));

    //----------------------
    File folder = new File("/Users/bsollish/dev/yaml_git_sync");
    File[] listOfFiles = folder.listFiles();

    if (listOfFiles != null) {
      for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isFile()) {
          System.out.println("File " + listOfFiles[i].getName());
        } else if (listOfFiles[i].isDirectory()) {
          System.out.println("Directory " + listOfFiles[i].getName());
        }
      }
    }
    //----------------------

    ConnectWithSshKey con = new ConnectWithSshKey();

    con.should_connect_to_public_ssh();
  }

  public void should_connect_to_public_ssh() throws IOException, GitAPIException {
    // final String REMOTE_URL = "git@github.com:lordofthejars/wildfly-example.git";
    final String REMOTE_URL = "git@github.com:wings-software/yml-test.git";

    SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
      @Override
      protected void configure(OpenSshConfig.Host host, Session session) {
        session.setUserInfo(new UserInfo() {
          @Override
          public String getPassphrase() {
            return "passphrase";
          }

          @Override
          public String getPassword() {
            return null;
          }

          @Override
          public boolean promptPassword(String message) {
            return false;
          }

          @Override
          public boolean promptPassphrase(String message) {
            return true;
          }

          @Override
          public boolean promptYesNo(String message) {
            return false;
          }

          @Override
          public void showMessage(String message) {}
        });
      }

      // FROM:
      // https://stackoverflow.com/questions/13686643/using-keys-with-jgit-to-access-a-git-repository-securely/19931041#19931041
      @Override
      protected JSch getJSch(final OpenSshConfig.Host hc, FS fs) throws JSchException {
        JSch jsch = super.getJSch(hc, fs);
        jsch.removeAllIdentity();
        // jsch.addIdentity( "/path/to/private/key" );
        jsch.addIdentity("~/.ssh/bobwings-GitHub.pub");
        // jsch.addIdentity( "~/.ssh/id_rsa.pub" );
        return jsch;
      }
    };

    File localPath = File.createTempFile("TestGitRepository", "");
    localPath.delete();

    try (Git result = Git.cloneRepository()
                          .setURI(REMOTE_URL)
                          .setTransportConfigCallback(transport -> {
                            SshTransport sshTransport = (SshTransport) transport;
                            sshTransport.setSshSessionFactory(sshSessionFactory);
                          })
                          .setDirectory(localPath)
                          .call()) {
      System.out.println("Having repository: " + result.getRepository().getDirectory());
    }
  }

  private String getFile(String fileName) {
    StringBuilder result = new StringBuilder("");

    // Get file from resources folder
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource(fileName).getFile());

    try (Scanner scanner = new Scanner(file)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        result.append(line).append("\n");
      }

      scanner.close();

    } catch (IOException e) {
      e.printStackTrace();
    }

    return result.toString();
  }
}

/*
  CloneCommand cloneCommand = Git.cloneRepository();
  cloneCommand.setURI( "ssh://user@example.com/repo.git" );
  cloneCommand.setTransportConfigCallback( new TransportConfigCallback() {
    @Override
    public void configure( Transport transport ) {
      SshTransport sshTransport = ( SshTransport )transport;
      sshTransport.setSshSessionFactory( sshSessionFactory );
    }
  } );
*/