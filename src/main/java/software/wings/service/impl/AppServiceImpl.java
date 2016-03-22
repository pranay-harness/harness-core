package software.wings.service.impl;

import java.util.Collections;
import java.util.List;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Metered;

import software.wings.beans.Application;
import software.wings.beans.ErrorConstants;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.dl.MongoHelper;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;

/**
 *  Application Service Implementation class.
 *
 *
 * @author Rishi
 *
 */
public class AppServiceImpl implements AppService {
  private Datastore datastore;

  public AppServiceImpl(Datastore datastore) {
    this.datastore = datastore;
  }

  @Override
  @Metered
  public Application save(Application app) {
    Key<Application> key = datastore.save(app);
    logger.debug("Key of the saved entity :" + key.toString());
    return datastore.get(Application.class, key.getId());
  }

  @Override
  public List<Application> list() {
    return datastore.find(Application.class).asList();
  }

  @Override
  public Application findByName(String appName) {
    Application app = datastore.find(Application.class, "name", appName).get();
    if (app == null) {
      throw new WingsException(Collections.singletonMap("appName", appName), ErrorConstants.INVALID_APP_NAME);
    }
    return app;
  }

  @Override
  public PageResponse<Application> list(PageRequest<Application> req) {
    return MongoHelper.queryPageRequest(datastore, Application.class, req);
  }

  private static Logger logger = LoggerFactory.getLogger(AppServiceImpl.class);

  @Override
  public Application findByUUID(String uuid) {
    return datastore.get(Application.class, uuid);
  }

  @Override
  public Application update(Application app) {
    return null;
  }
}
