package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface OwnedByActivity {
  /**
   * prune by activity.
   *
   * @param appId      the app id
   * @param activityId the activity id
   */
  void pruneByActivity(String appId, String activityId);
}
