package software.wings.service.impl;

import software.wings.beans.EntityType;
import software.wings.beans.History;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HistoryService;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 6/20/16.
 */
public class HistoryServiceImpl implements HistoryService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ExecutorService executorService;

  @Override
  public void create(History history) {
    wingsPersistence.save(history);
  }

  @Override
  public void createAsync(History history) {
    executorService.execute(() -> { create(history); });
  }

  @Override
  public PageResponse<History> list(PageRequest<History> request) {
    return wingsPersistence.query(History.class, request);
  }

  @Override
  public History get(String appId, String historyId) {
    History history = wingsPersistence.get(History.class, appId, historyId);
    if (history.getEntityType() == EntityType.ORCHESTRATED_DEPLOYMENT
        || history.getEntityType() == EntityType.SIMPLE_DEPLOYMENT) {
      history.setEntityNewValue(
          workflowExecutionService.getExecutionDetailsWithoutGraph(history.getAppId(), history.getEntityId()));
    }
    return history;
  }

  @Override
  public void deleteByApplication(String appId) {
    wingsPersistence.delete(wingsPersistence.createQuery(History.class).field("appId").equal(appId));
  }
}
