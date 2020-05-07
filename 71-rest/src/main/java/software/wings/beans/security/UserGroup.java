package software.wings.beans.security;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.common.collect.ImmutableSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.structure.CollectionUtils;
import io.harness.notifications.NotificationReceiverInfo;
import io.harness.persistence.AccountAccess;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.User;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.beans.sso.SSOType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * User bean class.
 *
 * @author Rishi
 */
@JsonInclude(NON_EMPTY)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Indexes({
  @Index(fields = { @Field("accountId")
                    , @Field("importedByScim") },
      options = @IndexOptions(name = "accountIdAndImportedByScim", background = true))

  ,
      @Index(fields = {
        @Field("accountId"), @Field("memberIds")
      }, options = @IndexOptions(name = "accountAndMemberIds", background = true))
})
@FieldNameConstants(innerTypeName = "UserGroupKeys")
@Entity(value = "userGroups", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class UserGroup extends Base implements NotificationReceiverInfo, AccountAccess {
  public static final String MEMBER_IDS_KEY = "memberIds";
  public static final String NAME_KEY = "name";
  public static final String ACCOUNT_ID_KEY = "accountId";
  public static final String IS_DEFAULT_KEY = "isDefault";
  public static final String NOTIFICATION_SETTINGS_KEY = "notificationSettings";

  /**
   * The constant DEFAULT_READ_ONLY_USER_GROUP_NAME.
   */
  public static final String DEFAULT_READ_ONLY_USER_GROUP_NAME = "ReadOnlyUserGroup";
  /**
   * The constant DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME.
   */
  public static final String DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME = "Non-Production Support";
  /**
   * The constant DEFAULT_PROD_SUPPORT_USER_GROUP_NAME.
   */
  public static final String DEFAULT_PROD_SUPPORT_USER_GROUP_NAME = "Production Support";
  /**
   * The constant DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME.
   */
  public static final String DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME = "Account Administrator";

  public static final Set<String> DEFAULT_USER_GROUPS =
      ImmutableSet.of(DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME, DEFAULT_PROD_SUPPORT_USER_GROUP_NAME,
          DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME, DEFAULT_READ_ONLY_USER_GROUP_NAME);

  @NotEmpty private String name;
  private String description;

  // TODO: User composition with SSOInfo class to store this info
  public static final String LINKED_SSO_ID_KEY = "linkedSsoId";
  private boolean isSsoLinked;
  private SSOType linkedSsoType;
  private String linkedSsoId;
  private String linkedSsoDisplayName;
  private String ssoGroupId;
  private String ssoGroupName;
  private boolean importedByScim;

  @Indexed private String accountId;
  private List<String> memberIds;

  @Transient private List<User> members;

  private Set<AppPermission> appPermissions;
  private AccountPermissions accountPermissions;

  @Nullable private NotificationSettings notificationSettings;

  private boolean isDefault;

  // TODO: Should use Builder at the class level itself.
  @Builder
  public UserGroup(String name, String description, String accountId, List<String> memberIds, List<User> members,
      Set<AppPermission> appPermissions, AccountPermissions accountPermissions, String uuid, String appId,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath,
      boolean isSsoLinked, SSOType linkedSsoType, String linkedSsoId, String linkedSsoDisplayName, String ssoGroupId,
      String ssoGroupName, NotificationSettings notificationSettings, boolean isDefault, boolean importedByScim) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.name = name;
    this.description = description;
    this.accountId = accountId;
    this.memberIds = memberIds;
    this.members = members;
    this.appPermissions = appPermissions;
    this.accountPermissions = accountPermissions;
    this.isSsoLinked = isSsoLinked;
    this.linkedSsoType = linkedSsoType;
    this.linkedSsoId = linkedSsoId;
    this.linkedSsoDisplayName = linkedSsoDisplayName;
    this.ssoGroupId = ssoGroupId;
    this.ssoGroupName = ssoGroupName;
    this.notificationSettings = notificationSettings;
    this.isDefault = isDefault;
    this.importedByScim = importedByScim;
  }

  public UserGroup cloneWithNewName(final String newName, final String newDescription) {
    return UserGroup.builder()
        .uuid(generateUuid())
        .appId(appId)
        .createdBy(null)
        .createdAt(0)
        .lastUpdatedBy(null)
        .lastUpdatedAt(0)
        // @@@ .keywords(getKeywords())
        .entityYamlPath(getEntityYamlPath())
        .name(newName)
        .description(newDescription)
        .accountId(accountId)
        .memberIds(memberIds)
        .members(members)
        .appPermissions(appPermissions)
        .accountPermissions(accountPermissions)
        .isSsoLinked(isSsoLinked)
        .linkedSsoType(linkedSsoType)
        .linkedSsoId(linkedSsoId)
        .linkedSsoDisplayName(linkedSsoDisplayName)
        .notificationSettings(notificationSettings)
        .isDefault(isDefault)
        .ssoGroupId(ssoGroupId)
        .ssoGroupName(ssoGroupName)
        .importedByScim(importedByScim)
        .build();
  }

  public List<User> getMembers() {
    return CollectionUtils.emptyIfNull(members);
  }

  @Override
  @JsonIgnore
  public Map<NotificationChannelType, List<String>> getAddressesByChannelType() {
    return null != notificationSettings ? notificationSettings.getAddressesByChannelType() : Collections.emptyMap();
  }

  @Nullable
  @Override
  @JsonIgnore
  public SlackNotificationSetting getSlackConfig() {
    return null != notificationSettings ? notificationSettings.getSlackConfig() : null;
  }

  @Nullable
  @Override
  @JsonIgnore
  public String getPagerDutyIntegrationKey() {
    return null != notificationSettings ? notificationSettings.getPagerDutyIntegrationKey() : null;
  }

  @Nullable
  @Override
  @JsonIgnore
  public String getMicrosoftTeamsWebhookUrl() {
    return null != notificationSettings ? notificationSettings.getMicrosoftTeamsWebhookUrl() : null;
  }

  @Override
  @JsonIgnore
  public List<String> getEmailAddresses() {
    return null != notificationSettings ? notificationSettings.getEmailAddresses() : Collections.emptyList();
  }

  @UtilityClass
  public static final class UserGroupKeys {
    // Temporary
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
    public static final String name = "name";
    public static final String accountId = "accountId";
    public static final String memberIds = "memberIds";
    public static final String importedByScim = "importedByScim";
  }
}
