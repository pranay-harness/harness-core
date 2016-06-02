package software.wings.beans;

/**
 * Created by anubhaw on 5/31/16.
 */
public class HostConnectionCredential {
  private String sshUser;
  private String sshPassword;
  private String appUser;
  private String appUserPassword;

  public String getSshUser() {
    return sshUser;
  }

  public void setSshUser(String sshUser) {
    this.sshUser = sshUser;
  }

  public String getSshPassword() {
    return sshPassword;
  }

  public void setSshPassword(String sshPassword) {
    this.sshPassword = sshPassword;
  }

  public String getAppUser() {
    return appUser;
  }

  public void setAppUser(String appUser) {
    this.appUser = appUser;
  }

  public String getAppUserPassword() {
    return appUserPassword;
  }

  public void setAppUserPassword(String appUserPassword) {
    this.appUserPassword = appUserPassword;
  }

  public static final class HostConnectionCredentialBuilder {
    private String sshUser;
    private String sshPassword;
    private String appUser;
    private String appUserPassword;

    private HostConnectionCredentialBuilder() {}

    public static HostConnectionCredentialBuilder aHostConnectionCredential() {
      return new HostConnectionCredentialBuilder();
    }

    public HostConnectionCredentialBuilder withSshUser(String sshUser) {
      this.sshUser = sshUser;
      return this;
    }

    public HostConnectionCredentialBuilder withSshPassword(String sshPassword) {
      this.sshPassword = sshPassword;
      return this;
    }

    public HostConnectionCredentialBuilder withAppUser(String appUser) {
      this.appUser = appUser;
      return this;
    }

    public HostConnectionCredentialBuilder withAppUserPassword(String appUserPassword) {
      this.appUserPassword = appUserPassword;
      return this;
    }

    public HostConnectionCredentialBuilder but() {
      return aHostConnectionCredential()
          .withSshUser(sshUser)
          .withSshPassword(sshPassword)
          .withAppUser(appUser)
          .withAppUserPassword(appUserPassword);
    }

    public HostConnectionCredential build() {
      HostConnectionCredential hostConnectionCredential = new HostConnectionCredential();
      hostConnectionCredential.setSshUser(sshUser);
      hostConnectionCredential.setSshPassword(sshPassword);
      hostConnectionCredential.setAppUser(appUser);
      hostConnectionCredential.setAppUserPassword(appUserPassword);
      return hostConnectionCredential;
    }
  }
}
