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

  @Transient private String accountName;

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

  @JsonIgnore
  public boolean isAccountAdmin(String accountId) {
    return roles != null
        && roles.stream()
               .filter(role
                   -> role.getRoleType() == RoleType.ACCOUNT_ADMIN && role.getAccountId() != null
                       && role.getAccountId().equals(accountId))
               .findFirst()
               .isPresent();
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

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;

    User user = (User) o;

    if (name != null ? !name.equals(user.name) : user.name != null)
      return false;
    if (email != null ? !email.equals(user.email) : user.email != null)
      return false;
    if (companyName != null ? !companyName.equals(user.companyName) : user.companyName != null)
      return false;
    return accountName != null ? accountName.equals(user.accountName) : user.accountName == null;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (email != null ? email.hashCode() : 0);
    result = 31 * result + (companyName != null ? companyName.hashCode() : 0);
    result = 31 * result + (accountName != null ? accountName.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "User{"
        + "name='" + name + '\'' + ", email='" + email + '\'' + ", companyName='" + companyName + '\''
        + ", accountName='" + accountName + '\'' + ", roles=" + roles + ", accounts=" + accounts
        + ", lastLogin=" + lastLogin + ", emailVerified=" + emailVerified + ", statsFetchedOn=" + statsFetchedOn + '}';
  }

  public static final class Builder {
    private String name;
    private String email;
    private String passwordHash;
    private String companyName;
    private String accountName;
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

    public Builder withAccountName(String accountName) {
      this.accountName = accountName;
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
          .withAccountName(accountName)
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
      user.setAccountName(accountName);
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
