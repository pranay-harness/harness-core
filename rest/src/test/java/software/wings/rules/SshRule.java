package software.wings.rules;

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.rules.MethodRule;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.Security;

/**
 * Created by peeyushaggarwal on 7/27/16.
 */
public class SshRule implements MethodRule {
  private SshServer sshd;
  private TemporaryFolder temporaryFolder;
  private String username = System.getProperty("user.name");
  private String password = "Wings@123";
  private Path keyPath;

  /**
   * Instantiates a new Ssh rule.
   *
   * @param temporaryFolder the temporary folder
   */
  public SshRule(TemporaryFolder temporaryFolder) {
    this.temporaryFolder = temporaryFolder;
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object o) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        SshRule.this.before();
        try {
          statement.evaluate();
        } finally {
          SshRule.this.after();
        }
      }
    };
  }

  /**
   * Gets port.
   *
   * @return the port
   */
  public int getPort() {
    return sshd.getPort();
  }

  /**
   * Getter for property 'username'.
   *
   * @return Value for property 'username'.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Setter for property 'username'.
   *
   * @param username Value to set for property 'username'.
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Getter for property 'password'.
   *
   * @return Value for property 'password'.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Setter for property 'password'.
   *
   * @param password Value to set for property 'password'.
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Getter for property 'keyPath'.
   *
   * @return Value for property 'keyPath'.
   */
  public Path getKeyPath() {
    return keyPath;
  }

  private void before() throws IOException {
    this.keyPath = new File(temporaryFolder.getRoot(), "key.pem").toPath();

    if (!SecurityUtils.isBouncyCastleRegistered()) {
      Security.addProvider(new BouncyCastleProvider());
    }
    sshd = SshServer.setUpDefaultServer();
    sshd.setPort(0);
    sshd.setKeyPairProvider(SecurityUtils.createGeneratorHostKeyProvider(keyPath));
    sshd.getKeyPairProvider().loadKeys();
    sshd.setPasswordAuthenticator(
        (username, password,
            session) -> StringUtils.equals(this.username, username) && StringUtils.equals(this.password, password));
    sshd.setFileSystemFactory(new VirtualFileSystemFactory(temporaryFolder.getRoot().toPath()));
    sshd.setPublickeyAuthenticator((username, key, session) -> StringUtils.equals("ssh_user", username));
    sshd.setCommandFactory(
        new ScpCommandFactory.Builder()
            .withDelegate(command -> new FileSystemAwareProcessShellFactory(command.split(" ")).create())
            .build());
    sshd.start();
  }

  private void after() throws IOException {
    sshd.stop(true);
  }
}
