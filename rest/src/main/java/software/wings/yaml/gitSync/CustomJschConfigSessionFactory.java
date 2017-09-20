package software.wings.yaml.gitSync;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;

public class CustomJschConfigSessionFactory extends JschConfigSessionFactory {
  private String passphrase;
  private String sshKeyPath;

  public CustomJschConfigSessionFactory(String passphrase, String sshKeyPath) {
    this.passphrase = passphrase;
    this.sshKeyPath = sshKeyPath;
  }

  @Override
  protected void configure(Host host, Session session) {
    // do nothing
  }

  @Override
  protected JSch createDefaultJSch(FS fs) throws JSchException {
    JSch defaultJSch = super.createDefaultJSch(fs);

    if (this.passphrase != null && !this.passphrase.isEmpty()) {
      defaultJSch.addIdentity(this.sshKeyPath, this.passphrase);
    } else {
      defaultJSch.addIdentity(this.sshKeyPath);
    }
    return defaultJSch;
  }

  public String getPassphrase() {
    return passphrase;
  }

  public void setPassphrase(String passphrase) {
    this.passphrase = passphrase;
  }

  public String getSshKeyPath() {
    return sshKeyPath;
  }

  public void setSshKeyPath(String sshKeyPath) {
    this.sshKeyPath = sshKeyPath;
  }
}
