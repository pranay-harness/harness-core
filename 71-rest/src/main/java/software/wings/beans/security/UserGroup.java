package software.wings.beans.security;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.User;
import software.wings.beans.sso.SSOType;

import java.util.List;
import java.util.Set;

/**
 * User bean class.
 *
 * @author Rishi
 */
@JsonInclude(NON_EMPTY)
@Entity(value = "userGroups", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class UserGroup extends Base {
  public static final String MEMBER_IDS_KEY = "memberIds";
  public static final String NAME_KEY = "name";

  @Indexed @NotEmpty private String name;
  private String description;

  // TODO: User composition with SSOInfo class to store this info
  public static final String LINKED_SSO_ID_KEY = "linkedSsoId";
  private boolean isSsoLinked;
  private SSOType linkedSsoType;
  private String linkedSsoId;
  private String linkedSsoDisplayName;
  private String ssoGroupId;
  private String ssoGroupName;

  @Indexed private String accountId;
  @Indexed private List<String> memberIds;
  @Transient private List<User> members;

  private Set<AppPermission> appPermissions;
  private AccountPermissions accountPermissions;

  // TODO: Should use Builder at the class level itself.
  @Builder
  public UserGroup(String name, String description, String accountId, List<String> memberIds, List<User> members,
      Set<AppPermission> appPermissions, AccountPermissions accountPermissions, String uuid, String appId,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords,
      String entityYamlPath, boolean isSsoLinked, SSOType linkedSsoType, String linkedSsoId,
      String linkedSsoDisplayName, String ssoGroupId, String ssoGroupName) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
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
  }

  public UserGroup cloneWithNewName(final String newName, final String newDescription) {
    return UserGroup.builder()
        .uuid(generateUuid())
        .appId(appId)
        .createdBy(null)
        .createdAt(0)
        .lastUpdatedBy(null)
        .lastUpdatedAt(0)
        .keywords(getKeywords())
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
        .ssoGroupId(ssoGroupId)
        .ssoGroupName(ssoGroupName)
        .build();
  }
}
