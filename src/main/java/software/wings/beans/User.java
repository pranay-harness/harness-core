package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
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
  private String name;

  @Indexed(unique = true) private String email;

  @JsonIgnore private String passwordHash;

  @Reference(idOnly = true, ignoreMissing = true) private List<Role> roles;

  private long lastLogin;

  @Transient private String password;
  @Transient private String token;

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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public long getLastLogin() {
    return lastLogin;
  }

  public void setLastLogin(long lastLogin) {
    this.lastLogin = lastLogin;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public List<Role> getRoles() {
    return roles;
  }

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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * The Class Builder.
   */
  public static class Builder {
    private String name;
    private String email;
    private String passwordHash;
    private List<Role> roles;
    private long lastLogin;
    private String password;
    private String token;
    private String uuid;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * An user.
     *
     * @return the builder
     */
    public static Builder anUser() {
      return new Builder();
    }

    /**
     * But.
     *
     * @return copy of builder object.
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
          .withUuid(uuid)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * With active.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * With last updated at.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With last updated by.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With created at.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With created by.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With uuid.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With token.
     *
     * @param token the token
     * @return the builder
     */
    public Builder withToken(String token) {
      this.token = token;
      return this;
    }

    /**
     * With password.
     *
     * @param password the password
     * @return the builder
     */
    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    /**
     * With last login.
     *
     * @param lastLogin the last login
     * @return the builder
     */
    public Builder withLastLogin(long lastLogin) {
      this.lastLogin = lastLogin;
      return this;
    }

    /**
     * With roles.
     *
     * @param roles the roles
     * @return the builder
     */
    public Builder withRoles(List<Role> roles) {
      this.roles = roles;
      return this;
    }

    /**
     * With password hash.
     *
     * @param passwordHash the password hash
     * @return the builder
     */
    public Builder withPasswordHash(String passwordHash) {
      this.passwordHash = passwordHash;
      return this;
    }

    /**
     * With email.
     *
     * @param email the email
     * @return the builder
     */
    public Builder withEmail(String email) {
      this.email = email;
      return this;
    }

    /**
     * With name.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * Builds the.
     *
     * @return Newly built User object.
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
      user.setUuid(uuid);
      user.setCreatedBy(createdBy);
      user.setCreatedAt(createdAt);
      user.setLastUpdatedBy(lastUpdatedBy);
      user.setLastUpdatedAt(lastUpdatedAt);
      user.setActive(active);
      return user;
    }
  }
}
