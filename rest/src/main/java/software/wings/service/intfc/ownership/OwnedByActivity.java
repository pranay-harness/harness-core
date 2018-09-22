package software.wings.service.intfc.ownership;

public interface OwnedByActivity {
  /**
   * prune by activity.
   *
   * @param appId      the app id
   * @param activityId the activity id
   */
  void pruneByActivity(String appId, String activityId);
}
