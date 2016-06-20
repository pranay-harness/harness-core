package software.wings.service.impl;

import com.google.inject.Inject;

import software.wings.beans.History;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HistoryService;

/**
 * Created by peeyushaggarwal on 6/20/16.
 */
public class HistoryServiceImpl implements HistoryService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void create(History history) {
    wingsPersistence.save(history);
  }

  @Override
  public PageResponse<History> list(PageRequest<History> request) {
    return wingsPersistence.query(History.class, request);
  }
}
