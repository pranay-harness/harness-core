package software.wings.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Metered;

import software.wings.beans.Application;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Application Service Implementation class.
 *
 * @author Rishi
 */
@Singleton
public class AppServiceImpl implements AppService {
  private static Logger logger = LoggerFactory.getLogger(AppServiceImpl.class);
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
  public PageResponse<Application> list(PageRequest<Application> req) {
    return wingsPersistence.query(Application.class, req);
  }

  @Override
  public Application findByUuid(String uuid) {
    return wingsPersistence.get(Application.class, uuid);
  }

  @Override
  public Application update(Application app) {
    return save(app);
  }
}
