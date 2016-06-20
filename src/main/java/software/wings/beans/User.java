package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

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
import javax.security.auth.Subject;

// TODO: Auto-generated Javadoc

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

  @Reference(idOnly = true, ignoreMissing = true) private List<Role> roles;

  private long lastLogin;

  @Transient private String password;
  @Transient private String token;

  private boolean emailVerified = false;

  /**
   * Return partial user object without sensitive information.
   *
   * @param fullUser Full User object.
   * @return Partial User object without sensitive information.
   */
  public static User getPublicUser(User fullUser) {
    User publicUser = new User();
    publicUser.setUuid(fullUser.getUuid());
    publicUser.setName(fullUser.getName());
    publicUser.setEmail(fullUser.getEmail());
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
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String email;
    private String passwordHash;
    private List<Role> roles;
    private long lastLogin;
    private String password;
    private String token;
    private boolean emailVerified = false;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

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
    public Builder withCreatedBy(User createdBy) {
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
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
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
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
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
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
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
      user.setUuid(uuid);
      user.setAppId(appId);
      user.setCreatedBy(createdBy);
      user.setCreatedAt(createdAt);
      user.setLastUpdatedBy(lastUpdatedBy);
      user.setLastUpdatedAt(lastUpdatedAt);
      user.setActive(active);
      return user;
    }
  }
}
