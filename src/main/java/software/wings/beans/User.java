package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.security.auth.Subject;

/**
 * User bean class.
 *
 * @author Rishi
 */
@JsonInclude(NON_EMPTY)
@Entity(value = "users", noClassnameStored = true)
public class User extends Base implements Principal {
  @NotEmpty private String name;

  @Indexed(unique = true) @Email private String email;

  @JsonIgnore private String passwordHash;

  @Reference(idOnly = true, ignoreMissing = true) private List<Role> roles = new ArrayList<>();

  private long lastLogin;

  @Transient private String password;
  @Transient private String token;

  private boolean emailVerified = false;

  private String companyName;

  private long statsFetchedOn;

  /**
   * Return partial user object without sensitive information.
   *
   * @return Partial User object without sensitive information.
   */
  @JsonIgnore
  public User getPublicUser() {
    User publicUser = new User();
    publicUser.setUuid(getUuid());
    publicUser.setName(getName());
    publicUser.setEmail(getEmail());
    publicUser.setCompanyName(getCompanyName());
    return publicUser;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /* (non-Javadoc)
   * @see java.security.Principal#implies(javax.security.auth.Subject)
   */
  @Override
  public boolean implies(Subject subject) {
    return false;
  }

  /**
   * Gets email.
   *
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets email.
   *
   * @param email the email
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Gets password hash.
   *
   * @return the password hash
   */
  public String getPasswordHash() {
    return passwordHash;
  }

  /**
   * Sets password hash.
   *
   * @param passwordHash the password hash
   */
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /**
   * Gets last login.
   *
   * @return the last login
   */
  public long getLastLogin() {
    return lastLogin;
  }

  /**
   * Sets last login.
   *
   * @param lastLogin the last login
   */
  public void setLastLogin(long lastLogin) {
    this.lastLogin = lastLogin;
  }

  /**
   * Gets token.
   *
   * @return the token
   */
  public String getToken() {
    return token;
  }

  /**
   * Sets token.
   *
   * @param token the token
   */
  public void setToken(String token) {
    this.token = token;
  }

  /**
   * Gets roles.
   *
   * @return the roles
   */
  public List<Role> getRoles() {
    return roles;
  }

  /**
   * Sets roles.
   *
   * @param roles the roles
   */
  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }

  /**
   * Adds role to User object.
   *
   * @param role role to assign to User.
   */
  public void addRole(Role role) {
    if (roles == null) {
      roles = new ArrayList<>();
    }
    roles.add(role);
  }

  /**
   * Gets password.
   *
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets password.
   *
   * @param password the password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Is email verified boolean.
   *
   * @return the boolean
   */
  public boolean isEmailVerified() {
    return emailVerified;
  }

  /**
   * Sets email verified.
   *
   * @param emailVerified the email verified
   */
  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  /**
   * Gets company name.
   *
   * @return the company name
   */
  public String getCompanyName() {
    return companyName;
  }

  /**
   * Sets company name.
   *
   * @param companyName the company name
   */
  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  /**
   * Gets stats fetched on.
   *
   * @return the stats fetched on
   */
  public long getStatsFetchedOn() {
    return statsFetchedOn;
  }

  /**
   * Sets stats fetched on.
   *
   * @param statsFetchedOn the stats fetched on
   */
  public void setStatsFetchedOn(long statsFetchedOn) {
    this.statsFetchedOn = statsFetchedOn;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(
              name, email, passwordHash, roles, lastLogin, password, token, emailVerified, companyName, statsFetchedOn);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final User other = (User) obj;
    return Objects.equals(this.name, other.name) && Objects.equals(this.email, other.email)
        && Objects.equals(this.passwordHash, other.passwordHash) && Objects.equals(this.roles, other.roles)
        && Objects.equals(this.lastLogin, other.lastLogin) && Objects.equals(this.password, other.password)
        && Objects.equals(this.token, other.token) && Objects.equals(this.emailVerified, other.emailVerified)
        && Objects.equals(this.companyName, other.companyName)
        && Objects.equals(this.statsFetchedOn, other.statsFetchedOn);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("email", email)
        .add("passwordHash", passwordHash)
        .add("roles", roles)
        .add("lastLogin", lastLogin)
        .add("password", password)
        .add("token", token)
        .add("emailVerified", emailVerified)
        .add("companyName", companyName)
        .add("statsFetchedOn", statsFetchedOn)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String email;
    private String passwordHash;
    private List<Role> roles = new ArrayList<>();
    private long lastLogin;
    private String password;
    private String token;
    private boolean emailVerified = false;
    private String companyName;
    private long statsFetchedOn;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * An user builder.
     *
     * @return the builder
     */
    public static Builder anUser() {
      return new Builder();
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With email builder.
     *
     * @param email the email
     * @return the builder
     */
    public Builder withEmail(String email) {
      this.email = email;
      return this;
    }

    /**
     * With password hash builder.
     *
     * @param passwordHash the password hash
     * @return the builder
     */
    public Builder withPasswordHash(String passwordHash) {
      this.passwordHash = passwordHash;
      return this;
    }

    /**
     * With roles builder.
     *
     * @param roles the roles
     * @return the builder
     */
    public Builder withRoles(List<Role> roles) {
      this.roles = roles;
      return this;
    }

    /**
     * With last login builder.
     *
     * @param lastLogin the last login
     * @return the builder
     */
    public Builder withLastLogin(long lastLogin) {
      this.lastLogin = lastLogin;
      return this;
    }

    /**
     * With password builder.
     *
     * @param password the password
     * @return the builder
     */
    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    /**
     * With token builder.
     *
     * @param token the token
     * @return the builder
     */
    public Builder withToken(String token) {
      this.token = token;
      return this;
    }

    /**
     * With email verified builder.
     *
     * @param emailVerified the email verified
     * @return the builder
     */
    public Builder withEmailVerified(boolean emailVerified) {
      this.emailVerified = emailVerified;
      return this;
    }

    /**
     * With company name builder.
     *
     * @param companyName the company name
     * @return the builder
     */
    public Builder withCompanyName(String companyName) {
      this.companyName = companyName;
      return this;
    }

    /**
     * With stats fetched on builder.
     *
     * @param statsFetchedOn the stats fetched on
     * @return the builder
     */
    public Builder withStatsFetchedOn(long statsFetchedOn) {
      this.statsFetchedOn = statsFetchedOn;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anUser()
          .withName(name)
          .withEmail(email)
          .withPasswordHash(passwordHash)
          .withRoles(roles)
          .withLastLogin(lastLogin)
          .withPassword(password)
          .withToken(token)
          .withEmailVerified(emailVerified)
          .withCompanyName(companyName)
          .withStatsFetchedOn(statsFetchedOn)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build user.
     *
     * @return the user
     */
    public User build() {
      User user = new User();
      user.setName(name);
      user.setEmail(email);
      user.setPasswordHash(passwordHash);
      user.setRoles(roles);
      user.setLastLogin(lastLogin);
      user.setPassword(password);
      user.setToken(token);
      user.setEmailVerified(emailVerified);
      user.setCompanyName(companyName);
      user.setStatsFetchedOn(statsFetchedOn);
      user.setUuid(uuid);
      user.setAppId(appId);
      user.setCreatedBy(createdBy);
      user.setCreatedAt(createdAt);
      user.setLastUpdatedBy(lastUpdatedBy);
      user.setLastUpdatedAt(lastUpdatedAt);
      return user;
    }
  }
}
