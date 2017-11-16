package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;

/**
 * User bean class.
 *
 * @author Rishi
 */
@JsonInclude(NON_EMPTY)
@Entity(value = "userGroups", noClassnameStored = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserGroup extends Base {
  @Indexed @NotEmpty private String name;
  private String description;

  @Indexed private String accountId;
  @Indexed private List<String> memberIds;
  @Transient private List<User> members;

  private List<AppPermission> appPermissions;
  private List<AccountPermissionType> accountPermissionTypes;
}
