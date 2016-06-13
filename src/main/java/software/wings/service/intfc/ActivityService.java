package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Activity;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;

import javax.validation.Valid;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public interface ActivityService {
  /**
   * List.
   *
   * @param appId       the app id
   * @param envId       the env id
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Activity> list(@NotEmpty String appId, @NotEmpty String envId, PageRequest<Activity> pageRequest);

  /**
   * Gets the.
   *
   * @param id    the id
   * @param appId the app id
   * @return the activity
   */
  Activity get(String id, String appId);

  /**
   * Save.
   *
   * @param activity the activity
   * @return the activity
   */
  @ValidationGroups(Create.class) Activity save(@Valid Activity activity);

  /**
   * Update status.
   *
   * @param activityId     the activity id
   * @param appId          the app id
   * @param activityStatus the activity status
   */
  void updateStatus(String activityId, String appId, Activity.Status activityStatus);
}
