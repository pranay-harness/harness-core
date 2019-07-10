package io.harness.dashboard;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author rktummala on 06/30/19
 */
@Value
@Builder
public class DashboardAccessPermissions {
  private List<String> userGroups;
  private List<Action> allowedActions;
}
