package software.wings.beans;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;
import software.wings.yaml.BaseEntityYaml;
import software.wings.yaml.BaseYaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by rishi on 10/30/16.
 */
@Entity(value = "notificationGroups", noClassnameStored = true)
@Indexes(
    @Index(options = @IndexOptions(name = "yaml", unique = true), fields = { @Field("accountId")
                                                                             , @Field("name") }))
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class NotificationGroup extends Base {
  @NotEmpty private String accountId;
  @NotNull private String name;
  private boolean editable = true;
  @Reference(idOnly = true, ignoreMissing = true) private List<Role> roles = new ArrayList<>();
  private boolean defaultNotificationGroupForAccount;
  @NotNull private Map<NotificationChannelType, List<String>> addressesByChannelType = new HashMap<>();

  /**
   * Gets name.
   *
   * @return the name
   */
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

  /**
   * Gets addresses by channel type.
   *
   * @return the addresses by channel type
   */
  public Map<NotificationChannelType, List<String>> getAddressesByChannelType() {
    return addressesByChannelType;
  }

  /**
   * Sets addresses by channel type.
   *
   * @param addressesByChannelType the addresses by channel type
   */
  public void setAddressesByChannelType(Map<NotificationChannelType, List<String>> addressesByChannelType) {
    this.addressesByChannelType = addressesByChannelType;
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
   * Editable or not
   * @return
   */
  public boolean isEditable() {
    return editable;
  }

  /**
   *
   * @param editable
   */
  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  public boolean isDefaultNotificationGroupForAccount() {
    return defaultNotificationGroupForAccount;
  }

  public void setDefaultNotificationGroupForAccount(boolean defaultNotificationGroupForAccount) {
    this.defaultNotificationGroupForAccount = defaultNotificationGroupForAccount;
  }

  /**
   *
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
   * The type Notification group builder.
   */
  public static final class NotificationGroupBuilder {
    private String accountId;
    private String name;
    private List<Role> roles = new ArrayList<>();
    private Map<NotificationChannelType, List<String>> addressesByChannelType = new HashMap<>();
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean editable;
    private boolean defaultNotificationGroupForAccount;

    private NotificationGroupBuilder() {}

    /**
     * Add addresses by channel type notification group builder.
     *
     * @param notificationChannelType the notification channel type
     * @param addresses               the addresses
     * @return the notification group builder
     */
    public NotificationGroupBuilder addAddressesByChannelType(
        NotificationChannelType notificationChannelType, List<String> addresses) {
      this.addressesByChannelType.put(notificationChannelType, addresses);
      return this;
    }

    /**
     * A notification group notification group builder.
     *
     * @return the notification group builder
     */
    public static NotificationGroupBuilder aNotificationGroup() {
      return new NotificationGroupBuilder();
    }

    /**
     * With account id notification group builder.
     *
     * @param accountId the account id
     * @return the notification group builder
     */
    public NotificationGroupBuilder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    /**
     * With
     * @param roles
     * @return the notification group builder
     */
    public NotificationGroupBuilder withRoles(List<Role> roles) {
      this.roles = roles;
      return this;
    }

    /**
     * With Role
     * @param role Role
     * @return the notification group builder
     */
    public NotificationGroupBuilder withRole(Role role) {
      this.roles.add(role);
      return this;
    }

    /**
     * With name notification group builder.
     *
     * @param name the name
     * @return the notification group builder
     */
    public NotificationGroupBuilder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With addresses by channel type notification group builder.
     *
     * @param addressesByChannelType the addresses by channel type
     * @return the notification group builder
     */
    public NotificationGroupBuilder withAddressesByChannelType(
        Map<NotificationChannelType, List<String>> addressesByChannelType) {
      this.addressesByChannelType = addressesByChannelType;
      return this;
    }

    /**
     * With uuid notification group builder.
     *
     * @param uuid the uuid
     * @return the notification group builder
     */
    public NotificationGroupBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id notification group builder.
     *
     * @param appId the app id
     * @return the notification group builder
     */
    public NotificationGroupBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by notification group builder.
     *
     * @param createdBy the created by
     * @return the notification group builder
     */
    public NotificationGroupBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at notification group builder.
     *
     * @param createdAt the created at
     * @return the notification group builder
     */
    public NotificationGroupBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by notification group builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the notification group builder
     */
    public NotificationGroupBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at notification group builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the notification group builder
     */
    public NotificationGroupBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public NotificationGroupBuilder withEditable(boolean editable) {
      this.editable = editable;
      return this;
    }

    public NotificationGroupBuilder withDefaultNotificationGroupForAccount(boolean defaultNotificationGroupForAccount) {
      this.defaultNotificationGroupForAccount = defaultNotificationGroupForAccount;
      return this;
    }

    /**
     * But notification group builder.
     *
     * @return the notification group builder
     */
    public NotificationGroupBuilder but() {
      return aNotificationGroup()
          .withAccountId(accountId)
          .withName(name)
          .withAddressesByChannelType(addressesByChannelType)
          .withRoles(roles)
          .withEditable(editable)
          .withDefaultNotificationGroupForAccount(defaultNotificationGroupForAccount)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build notification group.
     *
     * @return the notification group
     */
    public NotificationGroup build() {
      NotificationGroup notificationGroup = new NotificationGroup();
      notificationGroup.setAccountId(accountId);
      notificationGroup.setRoles(roles);
      notificationGroup.setName(name);
      notificationGroup.setAddressesByChannelType(addressesByChannelType);
      notificationGroup.setUuid(uuid);
      notificationGroup.setAppId(appId);
      notificationGroup.setCreatedBy(createdBy);
      notificationGroup.setCreatedAt(createdAt);
      notificationGroup.setLastUpdatedBy(lastUpdatedBy);
      notificationGroup.setLastUpdatedAt(lastUpdatedAt);
      notificationGroup.setEditable(editable);
      notificationGroup.setDefaultNotificationGroupForAccount(defaultNotificationGroupForAccount);
      return notificationGroup;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseEntityYaml {
    private List<AddressYaml> addresses;
    private String defaultNotificationGroupForAccount;

    @Builder
    public Yaml(
        String type, String harnessApiVersion, List<AddressYaml> addresses, String defaultNotificationGroupForAccount) {
      super(type, harnessApiVersion);
      this.addresses = addresses;
      this.defaultNotificationGroupForAccount = defaultNotificationGroupForAccount;
    }
  }

  /**
   * Yaml representation of addressesByChannelType in NotificationGroup.
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class AddressYaml extends BaseYaml {
    private String channelType;
    private List<String> addresses;

    @Builder
    public AddressYaml(String channelType, List<String> addresses) {
      this.channelType = channelType;
      this.addresses = addresses;
    }
  }
}
