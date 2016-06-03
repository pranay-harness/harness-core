package software.wings.service.impl;

import com.google.inject.Inject;

import software.wings.beans.Log;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.LogService;

import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Singleton
@ValidateOnExecution
public class LogServiceImpl implements LogService {
  @Inject private WingsPersistence wingsPersistence;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.LogService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Log> list(PageRequest<Log> pageRequest) {
    return wingsPersistence.query(Log.class, pageRequest);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.LogService#save(software.wings.beans.Log)
   */
  @Override
  public Log save(Log log) {
    wingsPersistence.save(log);
    return log;
  }
}
