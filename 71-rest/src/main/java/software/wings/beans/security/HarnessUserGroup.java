package software.wings.beans.security;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;
import software.wings.security.PermissionAttribute.Action;

import java.util.List;
import java.util.Set;

/**
 * User bean class.
 *
 * @author Rishi
 */
@JsonInclude(NON_EMPTY)
@Entity(value = "harnessUserGroups", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class HarnessUserGroup extends Base {
  @NotEmpty private String name;
  private String description;
  private Set<Action> actions;
  private boolean applyToAllAccounts;
  private Set<String> memberIds;
  private Set<String> accountIds;

  @Builder
  public HarnessUserGroup(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, String name, String description,
      Set<Action> actions, boolean applyToAllAccounts, Set<String> memberIds, Set<String> accountIds) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.name = name;
    this.description = description;
    this.actions = actions;
    this.applyToAllAccounts = applyToAllAccounts;
    this.memberIds = memberIds;
    this.accountIds = accountIds;
  }
}
