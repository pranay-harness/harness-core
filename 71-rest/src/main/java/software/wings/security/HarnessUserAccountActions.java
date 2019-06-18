package software.wings.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.wings.security.PermissionAttribute.Action;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This data structure carries all the actions allowed by as specific user on specific accounts as
 * specified in the "harness_user_groups" collection. This information will be encoded into the Identity
 * Service tokens and carried over when identity service calls into manager. Manager will use this information
 * to check if the API calls to the specific administrative type of APIs should be performed. This is
 * part of the effort to move 'harnessUserGroups' from manager level to identity service level.
 *
 * @author marklu on 2019-06-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarnessUserAccountActions {
  // Currently we only have harness user groups granting READ action against all accounts
  // We may add some harness user group to grant more actions/permissions against specific account
  // in the future.
  private boolean applyToAllAccounts;

  // Actions allowed on all account.
  private Set<Action> actions = new HashSet<>();

  // Specific actions allowed on specific account.
  private Map<String, Set<Action>> accountActions = new HashMap<>();
}
