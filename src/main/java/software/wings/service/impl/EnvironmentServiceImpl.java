package software.wings.service.impl;

import software.wings.beans.Environment;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.EnvironmentService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 4/1/16.
 */

@Singleton
public class EnvironmentServiceImpl implements EnvironmentService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Environment> listEnvironments(PageRequest<Environment> req) {
    return wingsPersistence.query(Environment.class, req);
  }

  @Override
  public Environment getEnvironments(String applicationId, String envName) {
    return null;
  }

  @Override
  public Environment createEnvironment(String applicationId, Environment environment) {
    environment.setApplicationId(applicationId);
    return wingsPersistence.saveAndGet(Environment.class, environment);
  }
}
