package software.wings.service.impl;

import com.google.inject.Inject;

import software.wings.beans.Activity;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ActivityService;

import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Singleton
@ValidateOnExecution
public class ActivityServiceImpl implements ActivityService {
  @Inject private WingsPersistence wingsPersistence;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ActivityService#list(java.lang.String, java.lang.String,
   * software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Activity> list(String appId, String envId, PageRequest<Activity> pageRequest) {
    pageRequest.addFilter("appId", appId, Operator.EQ);
    pageRequest.addFilter("environmentId", envId, Operator.EQ);

    return wingsPersistence.query(Activity.class, pageRequest);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ActivityService#get(java.lang.String, java.lang.String)
   */
  @Override
  public Activity get(String id, String appId) {
    return wingsPersistence.get(Activity.class, appId, id);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ActivityService#save(software.wings.beans.Activity)
   */
  @Override
  public Activity save(Activity activity) {
    wingsPersistence.save(activity);
    return activity;
  }
}
