package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.security.UserGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 3/6/17.
 */
@Entity(value = "userInvites", noClassnameStored = true)
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
//@Indexes(@Index(fields = {@Field("accountId"), @Field("email")}, options = @IndexOptions(unique = true))) //TODO:
// handle update with insert and then uncomment
public class UserInvite extends Base {
  @NotEmpty private String accountId;
  @NotEmpty(groups = {Update.class}) private String email;
  @Reference(idOnly = true, ignoreMissing = true) private List<Role> roles = new ArrayList<>();
  @Transient private List<UserGroup> userGroups = new ArrayList<>();
  private boolean completed;
  @Transient @JsonProperty(access = WRITE_ONLY) private List<String> emails = new ArrayList<>();

  @Transient private String name;

  @Transient private char[] password;

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
    return emails;
  }

  public void setEmails(List<String> emails) {
    this.emails = emails;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public char[] getPassword() {
    return password;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setPassword(char[] password) {
    this.password = password;
  }

  public static final class UserInviteBuilder {
    private String accountId;
    private String email;
    private List<Role> roles = new ArrayList<>();
    private List<UserGroup> userGroups = new ArrayList<>();
    private boolean completed;
    private List<String> emails = new ArrayList<>();
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private UserInviteBuilder() {}

    public static UserInviteBuilder anUserInvite() {
      return new UserInviteBuilder();
    }

    public UserInviteBuilder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public UserInviteBuilder withEmail(String email) {
      this.email = email;
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

    public UserInvite build() {
      UserInvite userInvite = new UserInvite();
      userInvite.setAccountId(accountId);
      userInvite.setEmail(email);
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
      return userInvite;
    }
  }
}
