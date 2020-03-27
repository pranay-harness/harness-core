package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.validation.Update;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.security.UserGroup;
import software.wings.beans.utm.UtmInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 3/6/17.
 */
@Entity(value = "userInvites", noClassnameStored = true)
@HarnessEntity(exportable = true)
@Indexes(@Index(fields = { @Field("accountId")
                           , @Field("email") }, options = @IndexOptions(name = "accountId_email_1")))
@FieldNameConstants(innerTypeName = "UserInviteKeys")
public class UserInvite extends Base {
  public static final String UUID_KEY = "uuid";

  private String accountId;
  @NotEmpty(groups = {Update.class}) private String email;
  @Reference(idOnly = true, ignoreMissing = true) private List<Role> roles = new ArrayList<>();
  @Transient private List<UserGroup> userGroups = new ArrayList<>();
  private boolean completed;
  @Transient @JsonProperty(access = WRITE_ONLY) private List<String> emails = new ArrayList<>();
  private UserInviteSource source = UserInviteSource.builder().build();

  // This flag denote if the user has agreed to the terms and conditions.
  private boolean agreement;

  private String name;

  private String givenName;

  private String familyName;

  @Getter @Setter @Transient private char[] password;
  @JsonIgnore private String passwordHash;

  private String accountName;

  private String companyName;

  private String marketPlaceToken;

  private boolean importedByScim;
  private UtmInfo utmInfo;

  @Override
  public boolean equals(Object o) {
    return super.equals(o) && true;
  }

  /**
   * Gets account id.
   *
   * @return the account id
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Sets account id.
   *
   * @param accountId the account id
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
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

  public List<UserGroup> getUserGroups() {
    return userGroups;
  }

  public void setUserGroups(List<UserGroup> userGroups) {
    this.userGroups = userGroups;
  }

  /**
   * Is complete boolean.
   *
   * @return the boolean
   */
  public boolean isCompleted() {
    return completed;
  }

  /**
   * Sets complete.
   *
   * @param completed the complete
   */
  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public List<String> getEmails() {
    return CollectionUtils.emptyIfNull(emails);
  }

  public void setEmails(List<String> emails) {
    this.emails = emails;
  }

  public String getName() {
    if (EmptyPredicate.isNotEmpty(name)) {
      return name;
    }
    return email.toLowerCase();
  }

  public String getGivenName() {
    if (EmptyPredicate.isNotEmpty(givenName)) {
      return givenName;
    }
    return name;
  }

  public String getFamilyName() {
    if (EmptyPredicate.isNotEmpty(familyName)) {
      return familyName;
    }
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  public UserInviteSource getSource() {
    return source;
  }

  public void setSource(UserInviteSource source) {
    this.source = source;
  }

  public boolean isAgreement() {
    return agreement;
  }

  public void setAgreement(boolean agreement) {
    this.agreement = agreement;
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public String getMarketPlaceToken() {
    return marketPlaceToken;
  }

  public void setMarketPlaceToken(String marketPlaceToken) {
    this.marketPlaceToken = marketPlaceToken;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public boolean getImportedByScim() {
    return importedByScim;
  }

  public void setImportedByScim(boolean importedByScim) {
    this.importedByScim = importedByScim;
  }

  public UtmInfo getUtmInfo() {
    return utmInfo;
  }

  public void setUtmInfo(UtmInfo utmInfo) {
    this.utmInfo = utmInfo;
  }

  public static final class UserInviteBuilder {
    private String accountId;
    private String email;
    private String name;
    private List<Role> roles = new ArrayList<>();
    private List<UserGroup> userGroups = new ArrayList<>();
    private boolean completed;
    private boolean agreement;
    private List<String> emails = new ArrayList<>();
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private String companyName;
    private String accountName;
    private String marketPlaceToken;
    private boolean importedByScim;
    private UserInviteSource source = UserInviteSource.builder().build();
    private UtmInfo utmInfo;
    private String givenName;
    private String familyName;

    private UserInviteBuilder() {}

    public static UserInviteBuilder anUserInvite() {
      return new UserInviteBuilder();
    }

    public UserInviteBuilder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public UserInviteBuilder withGivenName(String givenName) {
      this.givenName = givenName;
      return this;
    }

    public UserInviteBuilder withFamilyName(String familyName) {
      this.familyName = familyName;
      return this;
    }

    public UserInviteBuilder withEmail(String email) {
      this.email = email;
      return this;
    }

    public UserInviteBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public UserInviteBuilder withRoles(List<Role> roles) {
      this.roles = roles;
      return this;
    }

    public UserInviteBuilder withCompleted(boolean completed) {
      this.completed = completed;
      return this;
    }

    public UserInviteBuilder withAgreement(boolean agreement) {
      this.agreement = agreement;
      return this;
    }

    public UserInviteBuilder withEmails(List<String> emails) {
      this.emails = emails;
      return this;
    }

    public UserInviteBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public UserInviteBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public UserInviteBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public UserInviteBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public UserInviteBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public UserInviteBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public UserInviteBuilder withUserGroups(List<UserGroup> userGroups) {
      this.userGroups = userGroups;
      return this;
    }

    public UserInviteBuilder withSource(UserInviteSource userInviteSource) {
      this.source = userInviteSource;
      return this;
    }

    public UserInviteBuilder withCompanyName(String companyName) {
      this.companyName = companyName;
      return this;
    }

    public UserInviteBuilder withAccountName(String accountName) {
      this.accountName = accountName;
      return this;
    }

    public UserInviteBuilder withMarketPlaceToken(String marketPlaceToken) {
      this.marketPlaceToken = marketPlaceToken;
      return this;
    }

    public UserInviteBuilder withImportedByScim(boolean importedByScim) {
      this.importedByScim = importedByScim;
      return this;
    }

    public UserInviteBuilder withUtmInfo(UtmInfo utmInfo) {
      this.utmInfo = utmInfo;
      return this;
    }

    public UserInvite build() {
      UserInvite userInvite = new UserInvite();
      userInvite.setAccountId(accountId);
      userInvite.setEmail(email);
      userInvite.setName(name);
      userInvite.setRoles(roles);
      userInvite.setUserGroups(userGroups);
      userInvite.setCompleted(completed);
      userInvite.setEmails(emails);
      userInvite.setUuid(uuid);
      userInvite.setAppId(appId);
      userInvite.setCreatedBy(createdBy);
      userInvite.setCreatedAt(createdAt);
      userInvite.setLastUpdatedBy(lastUpdatedBy);
      userInvite.setLastUpdatedAt(lastUpdatedAt);
      userInvite.setSource(source);
      userInvite.setAgreement(agreement);
      userInvite.setAccountName(accountName);
      userInvite.setCompanyName(companyName);
      userInvite.setMarketPlaceToken(marketPlaceToken);
      userInvite.setImportedByScim(importedByScim);
      userInvite.setUtmInfo(utmInfo);
      userInvite.setFamilyName(familyName);
      userInvite.setGivenName(givenName);

      return userInvite;
    }
  }
}
