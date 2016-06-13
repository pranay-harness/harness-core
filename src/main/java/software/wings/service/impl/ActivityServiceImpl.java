package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

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

  @Override
  public PageResponse<Activity> list(String appId, String envId, PageRequest<Activity> pageRequest) {
    pageRequest.addFilter("appId", appId, Operator.EQ);
    pageRequest.addFilter("environmentId", envId, Operator.EQ);

    return wingsPersistence.query(Activity.class, pageRequest);
  }

  @Override
  public Activity get(String id, String appId) {
    return wingsPersistence.get(Activity.class, appId, id);
  }

  @Override
  public Activity save(Activity activity) {
    wingsPersistence.save(activity);
    return activity;
  }

  @Override
  public void updateStatus(String activityId, String appId, Activity.Status activityStatus) {
    wingsPersistence.update(
        wingsPersistence.createQuery(Activity.class).field(ID_KEY).equal(activityId).field("appId").equal(appId),
        wingsPersistence.createUpdateOperations(Activity.class).set("status", activityStatus));
  }
}
