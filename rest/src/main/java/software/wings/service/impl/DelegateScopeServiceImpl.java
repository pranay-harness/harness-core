package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Singleton;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateScope;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.DelegateService;

import java.util.List;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by brett on 8/4/17
 */
@Singleton
@ValidateOnExecution
public class DelegateScopeServiceImpl implements DelegateScopeService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DelegateService delegateService;

  @Override
  public PageResponse<DelegateScope> list(PageRequest<DelegateScope> pageRequest) {
    return wingsPersistence.query(DelegateScope.class, pageRequest);
  }

  @Override
  public DelegateScope get(String accountId, String delegateScopeId) {
    return wingsPersistence.get(DelegateScope.class,
        aPageRequest().addFilter("accountId", EQ, accountId).addFilter(ID_KEY, EQ, delegateScopeId).build());
  }

  @Override
  public DelegateScope update(DelegateScope delegateScope) {
    UpdateOperations<DelegateScope> updateOperations = wingsPersistence.createUpdateOperations(DelegateScope.class);
    setUnset(updateOperations, "name", delegateScope.getName());
    setUnset(updateOperations, "taskTypes", delegateScope.getTaskTypes());
    setUnset(updateOperations, "environmentTypes", delegateScope.getEnvironmentTypes());
    setUnset(updateOperations, "applications", delegateScope.getApplications());
    setUnset(updateOperations, "environments", delegateScope.getEnvironments());
    setUnset(updateOperations, "serviceInfrastructures", delegateScope.getServiceInfrastructures());

    logger.info("Updating delegate scope: {}", delegateScope.getUuid());
    Query<DelegateScope> query = wingsPersistence.createQuery(DelegateScope.class)
                                     .field("accountId")
                                     .equal(delegateScope.getAccountId())
                                     .field(ID_KEY)
                                     .equal(delegateScope.getUuid());
    wingsPersistence.update(query, updateOperations);
    DelegateScope updatedDelegateScope = get(delegateScope.getAccountId(), delegateScope.getUuid());

    List<Delegate> delegates = wingsPersistence.createQuery(Delegate.class)
                                   .field("accountId")
                                   .equal(updatedDelegateScope.getAccountId())
                                   .asList();
    for (Delegate delegate : delegates) {
      boolean includeUpdated = replaceUpdatedScope(updatedDelegateScope, delegate.getIncludeScopes());
      boolean excludeUpdated = replaceUpdatedScope(updatedDelegateScope, delegate.getExcludeScopes());
      if (includeUpdated || excludeUpdated) {
        delegateService.updateScopes(delegate);
      }
    }

    return updatedDelegateScope;
  }

  private boolean replaceUpdatedScope(DelegateScope updatedDelegateScope, List<DelegateScope> scopes) {
    if (scopes != null) {
      DelegateScope matchScope = null;
      int index = 0;
      for (DelegateScope scope : scopes) {
        if (scope.getUuid().equals(updatedDelegateScope.getUuid())) {
          matchScope = scope;
          break;
        }
        index++;
      }
      if (matchScope != null) {
        scopes.remove(matchScope);
        scopes.add(index, updatedDelegateScope);
        return true;
      }
    }
    return false;
  }

  @Override
  public DelegateScope add(DelegateScope delegateScope) {
    logger.info("Adding delegate scope: {}", delegateScope.getUuid());
    delegateScope.setAppId(GLOBAL_APP_ID);
    return wingsPersistence.saveAndGet(DelegateScope.class, delegateScope);
  }

  @Override
  public void delete(String accountId, String delegateScopeId) {
    logger.info("Deleting delegate scope: {}", delegateScopeId);
    wingsPersistence.delete(wingsPersistence.createQuery(DelegateScope.class)
                                .field("accountId")
                                .equal(accountId)
                                .field(ID_KEY)
                                .equal(delegateScopeId));
  }
}
