package io.harness.dashboard;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala on 06/30/19
 */
@Value
@Builder
public class DashboardAccessPermissions {
  private List<String> userGroups;
  private List<Action> allowedActions;
}
