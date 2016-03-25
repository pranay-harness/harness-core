package software.wings.service.impl;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Metered;

import software.wings.beans.Application;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;

/**
 *  Application Service Implementation class.
 *
 *
 * @author Rishi
 *
 */
@Singleton
public class AppServiceImpl implements AppService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  @Metered
  public Application save(Application app) {
    return wingsPersistence.saveAndGet(Application.class, app);
  }

  @Override
  public List<Application> list() {
    return wingsPersistence.list(Application.class);
  }

  @Override
  public Application findByName(String appName) {
    return null;
    //		Application app = datastore.find(Application.class, "name", appName).get();
    //		if (app==null) {
    //			throw new WingsException(Collections.singletonMap("appName", appName),
    //ErrorConstants.INVALID_APP_NAME);
    //		}
    //		return app;
  }

  @Override
  public PageResponse<Application> list(PageRequest<Application> req) {
    return wingsPersistence.query(Application.class, req);
  }

  private static Logger logger = LoggerFactory.getLogger(AppServiceImpl.class);

  @Override
  public Application findByUUID(String uuid) {
    return wingsPersistence.get(Application.class, uuid);
  }

  @Override
  public Application update(Application app) {
    return null;
  }
}