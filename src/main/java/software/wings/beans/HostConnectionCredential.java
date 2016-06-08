package software.wings.beans;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/31/16.
 */
public class HostConnectionCredential {
  private String sshUser;
  private String sshPassword;
  private String appUser;
  private String appUserPassword;

  /**
   * Gets ssh user.
   *
   * @return the ssh user
   */
  public String getSshUser() {
    return sshUser;
  }

  /**
   * Sets ssh user.
   *
   * @param sshUser the ssh user
   */
  public void setSshUser(String sshUser) {
    this.sshUser = sshUser;
  }

  /**
   * Gets ssh password.
   *
   * @return the ssh password
   */
  public String getSshPassword() {
    return sshPassword;
  }

  /**
   * Sets ssh password.
   *
   * @param sshPassword the ssh password
   */
  public void setSshPassword(String sshPassword) {
    this.sshPassword = sshPassword;
  }

  /**
   * Gets app user.
   *
   * @return the app user
   */
  public String getAppUser() {
    return appUser;
  }

  /**
   * Sets app user.
   *
   * @param appUser the app user
   */
  public void setAppUser(String appUser) {
    this.appUser = appUser;
  }

  /**
   * Gets app user password.
   *
   * @return the app user password
   */
  public String getAppUserPassword() {
    return appUserPassword;
  }

  /**
   * Sets app user password.
   *
   * @param appUserPassword the app user password
   */
  public void setAppUserPassword(String appUserPassword) {
    this.appUserPassword = appUserPassword;
  }

  /**
   * The Class HostConnectionCredentialBuilder.
   */
  public static final class HostConnectionCredentialBuilder {
    private String sshUser;
    private String sshPassword;
    private String appUser;
    private String appUserPassword;

    private HostConnectionCredentialBuilder() {}

    /**
     * A host connection credential.
     *
     * @return the host connection credential builder
     */
    public static HostConnectionCredentialBuilder aHostConnectionCredential() {
      return new HostConnectionCredentialBuilder();
    }

    /**
     * With ssh user.
     *
     * @param sshUser the ssh user
     * @return the host connection credential builder
     */
    public HostConnectionCredentialBuilder withSshUser(String sshUser) {
      this.sshUser = sshUser;
      return this;
    }

    /**
     * With ssh password.
     *
     * @param sshPassword the ssh password
     * @return the host connection credential builder
     */
    public HostConnectionCredentialBuilder withSshPassword(String sshPassword) {
      this.sshPassword = sshPassword;
      return this;
    }

    /**
     * With app user.
     *
     * @param appUser the app user
     * @return the host connection credential builder
     */
    public HostConnectionCredentialBuilder withAppUser(String appUser) {
      this.appUser = appUser;
      return this;
    }

    /**
     * With app user password.
     *
     * @param appUserPassword the app user password
     * @return the host connection credential builder
     */
    public HostConnectionCredentialBuilder withAppUserPassword(String appUserPassword) {
      this.appUserPassword = appUserPassword;
      return this;
    }

    /**
     * But.
     *
     * @return the host connection credential builder
     */
    public HostConnectionCredentialBuilder but() {
      return aHostConnectionCredential()
          .withSshUser(sshUser)
          .withSshPassword(sshPassword)
          .withAppUser(appUser)
          .withAppUserPassword(appUserPassword);
    }

    /**
     * Builds the.
     *
     * @return the host connection credential
     */
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
