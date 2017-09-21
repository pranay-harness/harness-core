package software.wings.yaml.gitSync;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.CredentialsProviderUserInfo;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.URIish;
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
    CredentialsProvider provider = new CredentialsProvider() {
      @Override
      public boolean isInteractive() {
        return false;
      }

      @Override
      public boolean supports(CredentialItem... items) {
        return true;
      }

      @Override
      public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        for (CredentialItem item : items) {
          ((CredentialItem.StringType) item).setValue(passphrase);
        }
        return true;
      }
    };
    UserInfo userInfo = new CredentialsProviderUserInfo(session, provider);
    session.setUserInfo(userInfo);
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
