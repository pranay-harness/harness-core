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

  @Transient private String companyName;

  @Reference(idOnly = true, ignoreMissing = true) private List<Role> roles = new ArrayList<>();

  @Reference(idOnly = true, ignoreMissing = true) private List<Account> accounts = new ArrayList<>();

  private long lastLogin;

  @Transient private String password;
  @Transient private String token;

  private boolean emailVerified = false;

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
    publicUser.setAccounts(getAccounts());
    // publicUser.setCompanyName(getCompanyName());
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

  /**
   * Getter for property 'accounts'.
   *
   * @return Value for property 'accounts'.
   */
  public List<Account> getAccounts() {
    return accounts;
  }

  /**
   * Setter for property 'accounts'.
   *
   * @param accounts Value to set for property 'accounts'.
   */
  public void setAccounts(List<Account> accounts) {
    this.accounts = accounts;
  }

  /**
   * Getter for property 'companyName'.
   *
   * @return Value for property 'companyName'.
   */
  public String getCompanyName() {
    return companyName;
  }

  /**
   * Setter for property 'companyName'.
   *
   * @param companyName Value to set for property 'companyName'.
   */
  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(
              name, email, passwordHash, roles, accounts, lastLogin, password, token, emailVerified, statsFetchedOn);
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
        && Objects.equals(this.accounts, other.accounts) && Objects.equals(this.lastLogin, other.lastLogin)
        && Objects.equals(this.password, other.password) && Objects.equals(this.token, other.token)
        && Objects.equals(this.emailVerified, other.emailVerified)
        && Objects.equals(this.statsFetchedOn, other.statsFetchedOn);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("email", email)
        .add("passwordHash", passwordHash)
        .add("roles", roles)
        .add("accounts", accounts)
        .add("lastLogin", lastLogin)
        .add("password", password)
        .add("token", token)
        .add("emailVerified", emailVerified)
        .add("statsFetchedOn", statsFetchedOn)
        .toString();
  }

  public static final class Builder {
    private String name;
    private String email;
    private String passwordHash;
    private String companyName;
    private List<Role> roles = new ArrayList<>();
    private List<Account> accounts = new ArrayList<>();
    private long lastLogin;
    private String password;
    private String token;
    private boolean emailVerified = false;
    private long statsFetchedOn;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    public static Builder anUser() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withEmail(String email) {
      this.email = email;
      return this;
    }

    public Builder withPasswordHash(String passwordHash) {
      this.passwordHash = passwordHash;
      return this;
    }

    public Builder withCompanyName(String companyName) {
      this.companyName = companyName;
      return this;
    }

    public Builder withRoles(List<Role> roles) {
      this.roles = roles;
      return this;
    }

    public Builder withAccounts(List<Account> accounts) {
      this.accounts = accounts;
      return this;
    }

    public Builder withLastLogin(long lastLogin) {
      this.lastLogin = lastLogin;
      return this;
    }

    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    public Builder withToken(String token) {
      this.token = token;
      return this;
    }

    public Builder withEmailVerified(boolean emailVerified) {
      this.emailVerified = emailVerified;
      return this;
    }

    public Builder withStatsFetchedOn(long statsFetchedOn) {
      this.statsFetchedOn = statsFetchedOn;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder but() {
      return anUser()
          .withName(name)
          .withEmail(email)
          .withPasswordHash(passwordHash)
          .withCompanyName(companyName)
          .withRoles(roles)
          .withAccounts(accounts)
          .withLastLogin(lastLogin)
          .withPassword(password)
          .withToken(token)
          .withEmailVerified(emailVerified)
          .withStatsFetchedOn(statsFetchedOn)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public User build() {
      User user = new User();
      user.setName(name);
      user.setEmail(email);
      user.setPasswordHash(passwordHash);
      user.setCompanyName(companyName);
      user.setRoles(roles);
      user.setAccounts(accounts);
      user.setLastLogin(lastLogin);
      user.setPassword(password);
      user.setToken(token);
      user.setEmailVerified(emailVerified);
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
