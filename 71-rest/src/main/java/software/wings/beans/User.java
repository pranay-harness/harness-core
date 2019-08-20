package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.beans.EmbeddedUser;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.loginSettings.UserLockoutInfo;
import software.wings.beans.security.UserGroup;
import software.wings.security.UserRequestContext;
import software.wings.security.UserRequestInfo;
import software.wings.security.authentication.TwoFactorAuthenticationMechanism;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * User bean class.
 *
 * @author Rishi
 */
@JsonInclude(NON_EMPTY)
@Entity(value = "users", noClassnameStored = true)
@HarnessExportableEntity
@FieldNameConstants(innerTypeName = "UserKeys")
public class User extends Base implements Principal {
  public static final String EMAIL_KEY = "email";
  public static final String ROLES_KEY = "roles";
  public static final String EMAIL_VERIFIED_KEY = "emailVerified";

  @NotEmpty private String name;

  @Indexed(options = @IndexOptions(unique = true)) @Email private String email;

  @JsonIgnore private String passwordHash;

  @Transient private String companyName;

  @Transient private String accountName;

  @JsonIgnore @Reference(idOnly = true, ignoreMissing = true) private List<Role> roles = new ArrayList<>();

  @Transient private List<UserGroup> userGroups = new ArrayList<>();

  @Reference(idOnly = true, ignoreMissing = true) private List<Account> accounts = new ArrayList<>();

  @Transient private List<Account> supportAccounts = new ArrayList<>();

  private long lastLogin;

  @Transient private boolean firstLogin;

  @Getter @Setter @Transient private char[] password;
  @Transient private String token;
  @Transient private String twoFactorJwtToken;

  private boolean emailVerified;

  private boolean passwordExpired;

  private boolean userLocked;

  private long statsFetchedOn;

  private String lastAccountId;

  private String defaultAccountId;

  private String lastAppId;

  private boolean disabled;

  private boolean imported;

  private UserLockoutInfo userLockoutInfo = new UserLockoutInfo();

  @JsonIgnore private long passwordChangedAt;

  @JsonIgnore @Transient private UserRequestInfo userRequestInfo;
  @JsonIgnore @Transient private UserRequestContext userRequestContext;

  private boolean twoFactorAuthenticationEnabled;
  private TwoFactorAuthenticationMechanism twoFactorAuthenticationMechanism;
  @JsonIgnore private String totpSecretKey;
  @JsonIgnore private long marketoLeadId;
  @JsonIgnore private String segmentIdentity;
  private String oauthProvider;
  @JsonIgnore private Set<String> reportedMarketoCampaigns = new HashSet<>();
  private Set<String> reportedSegmentTracks = new HashSet<>();

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
    publicUser.setDefaultAccountId(getDefaultAccountId());
    publicUser.setAccounts(getAccounts());
    publicUser.setSupportAccounts(getSupportAccounts());
    publicUser.setTwoFactorAuthenticationEnabled(isTwoFactorAuthenticationEnabled());
    publicUser.setTwoFactorAuthenticationMechanism(getTwoFactorAuthenticationMechanism());
    publicUser.setFirstLogin(isFirstLogin());
    publicUser.setLastLogin(getLastLogin());
    publicUser.setPasswordExpired(isPasswordExpired());
    publicUser.setUserLocked(isUserLocked());
    publicUser.setImported(isImported());
    // publicUser.setCompanyName(getCompanyName());
    return publicUser;
  }

  public boolean isAccountAdmin(String accountId) {
    return roles != null
        && roles.stream()
               .filter(role
                   -> role.getRoleType() == RoleType.ACCOUNT_ADMIN && role.getAccountId() != null
                       && role.getAccountId().equals(accountId))
               .findFirst()
               .isPresent();
  }

  public boolean isAllAppAdmin(String accountId) {
    return roles != null
        && roles.stream()
               .filter(role
                   -> role.getRoleType() == RoleType.APPLICATION_ADMIN && role.getAccountId() != null
                       && role.getAccountId().equals(accountId) && role.isAllApps())
               .findFirst()
               .isPresent();
  }

  public boolean isAppAdmin(String accountId, String appId) {
    return roles != null
        && roles.stream()
               .filter(role
                   -> role.getRoleType() == RoleType.APPLICATION_ADMIN && role.getAccountId() != null
                       && role.getAccountId().equals(accountId) && (role.isAllApps() || appId.equals(role.getAppId())))
               .findFirst()
               .isPresent();
  }

  @JsonIgnore
  public List<Role> getRolesByAccountId(String accountId) {
    if (roles == null) {
      return null;
    }
    return roles.stream()
        .filter(role -> role.getAccountId() != null && role.getAccountId().equals(accountId))
        .collect(toList());
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

  public boolean isImported() {
    return imported;
  }

  public void setImported(boolean imported) {
    this.imported = imported;
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

  public boolean isPasswordExpired() {
    return passwordExpired;
  }

  public void setPasswordExpired(boolean passwordExpired) {
    this.passwordExpired = passwordExpired;
  }

  public boolean isUserLocked() {
    return userLocked;
  }

  public void setUserLocked(boolean userLocked) {
    this.userLocked = userLocked;
  }

  public UserLockoutInfo getUserLockoutInfo() {
    return userLockoutInfo;
  }

  public void setUserLockoutInfo(UserLockoutInfo userLockoutInfo) {
    this.userLockoutInfo = userLockoutInfo;
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

  public boolean isFirstLogin() {
    return firstLogin;
  }

  public void setFirstLogin(boolean firstLogin) {
    this.firstLogin = firstLogin;
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

  public String getLastAccountId() {
    if (isEmpty(lastAccountId)) {
      if (isNotEmpty(accounts)) {
        // The first account will be considered as last account if not set. It will be used for encoding AuthToken.
        lastAccountId = accounts.get(0).getUuid();
      }
    }
    return lastAccountId;
  }

  public void setLastAccountId(String lastAccountId) {
    this.lastAccountId = lastAccountId;
  }

  public String getDefaultAccountId() {
    if (defaultAccountId == null && isNotEmpty(accounts)) {
      defaultAccountId = accounts.get(0).getUuid();
    }
    return defaultAccountId;
  }

  public void setDefaultAccountId(String accountId) {
    this.defaultAccountId = accountId;
  }

  public String getLastAppId() {
    return lastAppId;
  }

  public void setLastAppId(String lastAppId) {
    this.lastAppId = lastAppId;
  }

  public UserRequestInfo getUserRequestInfo() {
    return userRequestInfo;
  }

  public void setUserRequestInfo(UserRequestInfo userRequestInfo) {
    this.userRequestInfo = userRequestInfo;
  }

  public List<Account> getSupportAccounts() {
    return supportAccounts;
  }

  public void setSupportAccounts(List<Account> supportAccounts) {
    this.supportAccounts = supportAccounts;
  }

  public List<UserGroup> getUserGroups() {
    return userGroups;
  }

  public void setUserGroups(List<UserGroup> userGroups) {
    this.userGroups = userGroups;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  @JsonIgnore
  public long getMarketoLeadId() {
    return marketoLeadId;
  }

  @JsonIgnore
  public void setMarketoLeadId(long marketoLeadId) {
    this.marketoLeadId = marketoLeadId;
  }

  @JsonIgnore
  public String getSegmentIdentity() {
    return segmentIdentity;
  }

  @JsonIgnore
  public void setSegmentIdentity(String segmentIdentity) {
    this.segmentIdentity = segmentIdentity;
  }

  @JsonIgnore
  public Set<String> getReportedMarketoCampaigns() {
    return reportedMarketoCampaigns;
  }

  @JsonIgnore
  public void setReportedMarketoCampaigns(Set<String> reportedMarketoCampaigns) {
    this.reportedMarketoCampaigns = reportedMarketoCampaigns;
  }

  public Set<String> getReportedSegmentTracks() {
    return reportedSegmentTracks;
  }

  public void setReportedSegmentTracks(Set<String> reportedSegmentTracks) {
    this.reportedSegmentTracks = reportedSegmentTracks;
  }

  public String getOauthProvider() {
    return oauthProvider;
  }

  public void setOauthProvider(String oauthProvider) {
    this.oauthProvider = oauthProvider;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    User user = (User) o;

    if (name != null ? !name.equals(user.name) : user.name != null) {
      return false;
    }
    if (email != null ? !email.equals(user.email) : user.email != null) {
      return false;
    }
    if (companyName != null ? !companyName.equals(user.companyName) : user.companyName != null) {
      return false;
    }
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

  public long getPasswordChangedAt() {
    return passwordChangedAt;
  }

  public void setPasswordChangedAt(long passwordChangedAt) {
    this.passwordChangedAt = passwordChangedAt;
  }

  public UserRequestContext getUserRequestContext() {
    return userRequestContext;
  }

  public void setUserRequestContext(UserRequestContext userRequestContext) {
    this.userRequestContext = userRequestContext;
  }

  public boolean isTwoFactorAuthenticationEnabled() {
    return twoFactorAuthenticationEnabled;
  }

  public void setTwoFactorAuthenticationEnabled(boolean twoFactorAuthenticationEnabled) {
    this.twoFactorAuthenticationEnabled = twoFactorAuthenticationEnabled;
  }

  public TwoFactorAuthenticationMechanism getTwoFactorAuthenticationMechanism() {
    return twoFactorAuthenticationMechanism;
  }

  public void setTwoFactorAuthenticationMechanism(TwoFactorAuthenticationMechanism twoFactorAuthenticationMechanism) {
    this.twoFactorAuthenticationMechanism = twoFactorAuthenticationMechanism;
  }

  public String getTotpSecretKey() {
    return totpSecretKey;
  }

  public void setTotpSecretKey(String totpSecretKey) {
    this.totpSecretKey = totpSecretKey;
  }

  public String getTwoFactorJwtToken() {
    return twoFactorJwtToken;
  }

  public void setTwoFactorJwtToken(String twoFactorJwtToken) {
    this.twoFactorJwtToken = twoFactorJwtToken;
  }

  public static final class Builder {
    private String name;
    private String email;
    private String passwordHash;
    private String companyName;
    private String accountName;
    private List<Role> roles = new ArrayList<>();
    private List<UserGroup> userGroups = new ArrayList<>();
    private List<Account> accounts = new ArrayList<>();
    private List<Account> supportAccounts = new ArrayList<>();
    private String defaultAccountId;
    private long lastLogin;
    private boolean firstLogin;
    private char[] password;
    private String token;
    private boolean emailVerified;
    private long statsFetchedOn;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean twoFactorAuthenticationEnabled;
    private TwoFactorAuthenticationMechanism twoFactorAuthenticationMechanism;
    private String totpSecretKey;
    private String twoFactorJwtToken;
    private String oauthProvider;
    private boolean passwordExpired;
    private boolean userLocked;
    private boolean imported;

    private Builder() {}

    public static Builder anUser() {
      return new Builder();
    }

    public Builder withPasswordExpired(boolean passwordExpired) {
      this.passwordExpired = passwordExpired;
      return this;
    }

    public Builder withUserLocked(boolean userLocked) {
      this.userLocked = userLocked;
      return this;
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

    public Builder withUserGroups(List<UserGroup> userGroups) {
      this.userGroups = userGroups;
      return this;
    }

    public Builder withAccounts(List<Account> accounts) {
      this.accounts = accounts;
      return this;
    }

    public Builder withSupportAccounts(List<Account> supportAccounts) {
      this.supportAccounts = supportAccounts;
      return this;
    }

    public Builder withDefaultAccountId(String accountId) {
      this.defaultAccountId = accountId;
      return this;
    }

    public Builder withLastLogin(long lastLogin) {
      this.lastLogin = lastLogin;
      return this;
    }

    public Builder withPassword(char[] password) {
      this.password = password == null ? null : password.clone();
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

    public Builder withFirstLogin(boolean firstLogin) {
      this.firstLogin = firstLogin;
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

    public Builder withTwoFactorAuthenticationEnabled(boolean twoFactorAuthenticationEnabled) {
      this.twoFactorAuthenticationEnabled = twoFactorAuthenticationEnabled;
      return this;
    }

    public Builder withTwoFactorAuthenticationMechanism(
        TwoFactorAuthenticationMechanism twoFactorAuthenticationMechanism) {
      this.twoFactorAuthenticationMechanism = twoFactorAuthenticationMechanism;
      return this;
    }

    public Builder withTotpSecretKey(String totpSecretKey) {
      this.totpSecretKey = totpSecretKey;
      return this;
    }

    public Builder withTwoFactorJwtToken(String twoFactorJwtToken) {
      this.twoFactorJwtToken = twoFactorJwtToken;
      return this;
    }

    public Builder withOauthProvider(String oauthProvider) {
      this.oauthProvider = oauthProvider;
      return this;
    }

    public Builder withImported(boolean imported) {
      this.imported = imported;
      return this;
    }

    public Builder but() {
      return anUser()
          .withName(name)
          .withEmail(email)
          .withPasswordHash(passwordHash)
          .withCompanyName(companyName)
          .withRoles(roles)
          .withUserGroups(userGroups)
          .withAccounts(accounts)
          .withSupportAccounts(supportAccounts)
          .withDefaultAccountId(defaultAccountId)
          .withLastLogin(lastLogin)
          .withPassword(password)
          .withToken(token)
          .withEmailVerified(emailVerified)
          .withFirstLogin(firstLogin)
          .withStatsFetchedOn(statsFetchedOn)
          .withUuid(uuid)
          .withAccountName(accountName)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withTwoFactorAuthenticationEnabled(twoFactorAuthenticationEnabled)
          .withTwoFactorAuthenticationMechanism(twoFactorAuthenticationMechanism)
          .withTotpSecretKey(totpSecretKey)
          .withTwoFactorJwtToken(twoFactorJwtToken)
          .withPasswordExpired(passwordExpired)
          .withOauthProvider(oauthProvider)
          .withImported(imported);
    }

    public User build() {
      User user = new User();
      user.setName(name);
      user.setEmail(email);
      user.setPasswordHash(passwordHash);
      user.setCompanyName(companyName);
      user.setAccountName(accountName);
      user.setRoles(roles);
      user.setUserGroups(userGroups);
      user.setAccounts(accounts);
      user.setSupportAccounts(supportAccounts);
      user.setDefaultAccountId(defaultAccountId);
      user.setLastLogin(lastLogin);
      user.setPassword(password);
      user.setToken(token);
      user.setEmailVerified(emailVerified);
      user.setFirstLogin(firstLogin);
      user.setStatsFetchedOn(statsFetchedOn);
      user.setUuid(uuid);
      user.setAppId(appId);
      user.setCreatedBy(createdBy);
      user.setCreatedAt(createdAt);
      user.setLastUpdatedBy(lastUpdatedBy);
      user.setLastUpdatedAt(lastUpdatedAt);
      user.setTwoFactorAuthenticationEnabled(twoFactorAuthenticationEnabled);
      user.setTwoFactorAuthenticationMechanism(twoFactorAuthenticationMechanism);
      user.setTotpSecretKey(totpSecretKey);
      user.setTwoFactorJwtToken(twoFactorJwtToken);
      user.setOauthProvider(oauthProvider);
      user.setPasswordExpired(passwordExpired);
      user.setUserLocked(userLocked);
      user.setImported(imported);
      return user;
    }
  }
}
