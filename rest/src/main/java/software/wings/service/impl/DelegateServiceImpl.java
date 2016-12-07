package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Status;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateService;
import software.wings.waitnotify.WaitNotifyEngine;

import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
public class DelegateServiceImpl implements DelegateService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public PageResponse<Delegate> list(PageRequest<Delegate> pageRequest) {
    return wingsPersistence.query(Delegate.class, pageRequest);
  }

  @Override
  public Delegate get(String accountId, String delegateId) {
    return wingsPersistence.get(
        Delegate.class, aPageRequest().addFilter("accountId", EQ, accountId).addFilter(ID_KEY, EQ, delegateId).build());
  }

  @Override
  public Delegate update(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = wingsPersistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, "status", delegate.getStatus());
    setUnset(updateOperations, "lastHeartBeat", delegate.getLastHeartBeat());

    wingsPersistence.update(wingsPersistence.createQuery(Delegate.class)
                                .field("accountId")
                                .equal(delegate.getAccountId())
                                .field(ID_KEY)
                                .equal(delegate.getUuid()),
        updateOperations);
    return get(delegate.getAccountId(), delegate.getUuid());
  }

  @Override
  public Delegate add(Delegate delegate) {
    delegate.setAppId(GLOBAL_APP_ID);
    return wingsPersistence.saveAndGet(Delegate.class, delegate);
  }

  @Override
  public void delete(String accountId, String delegateId) {
    wingsPersistence.delete(wingsPersistence.createQuery(Delegate.class)
                                .field("accountId")
                                .equal(accountId)
                                .field(ID_KEY)
                                .equal(delegateId));
  }

  @Override
  public Delegate register(Delegate delegate) {
    Delegate existingDelegate = wingsPersistence.get(Delegate.class,
        aPageRequest()
            .addFilter("ip", EQ, delegate.getIp())
            .addFilter("hostName", EQ, delegate.getHostName())
            .addFilter("accountId", EQ, delegate.getAccountId())
            .build());

    if (existingDelegate == null) {
      return add(delegate);
    } else {
      delegate.setUuid(existingDelegate.getUuid());
      delegate.setStatus(existingDelegate.getStatus() == Status.DISABLED ? Status.DISABLED : delegate.getStatus());
      return update(delegate);
    }
  }
  /*
    public void sendTaskWaitNotify(DelegateTask task) {
      wingsPersistence.save(task);
    }

    public void processDelegateResponse(DelegateTaskResponse response) {
      DelegateTask delegateTask = wingsPersistence.get(DelegateTask.class, response.getAppId(), response.getTaskId());
      String waitId = delegateTask.getWaitId();
      waitNotifyEngine.notify(waitId, response.getResponse());
    }
  */
}
