package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;
import static io.harness.mongo.MongoUtils.setUnset;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.DelegateScope;
import software.wings.beans.DelegateScope.DelegateScopeKeys;
import software.wings.beans.Event;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.DelegateService;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DuplicateKeyException;
import java.util.List;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateScopeServiceImpl implements DelegateScopeService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DelegateService delegateService;
  @Inject private AuditServiceHelper auditServiceHelper;

  @Override
  public PageResponse<DelegateScope> list(PageRequest<DelegateScope> pageRequest) {
    return wingsPersistence.query(DelegateScope.class, pageRequest);
  }

  @Override
  public DelegateScope get(String accountId, String delegateScopeId) {
    return wingsPersistence.createQuery(DelegateScope.class)
        .filter(DelegateScopeKeys.uuid, delegateScopeId)
        .filter(DelegateScopeKeys.accountId, accountId)
        .get();
  }

  @Override
  public DelegateScope update(DelegateScope delegateScope) {
    if (!delegateScope.isValid()) {
      throw new InvalidArgumentsException("Delegate scope cannot be empty.", USER);
    }
    UpdateOperations<DelegateScope> updateOperations = wingsPersistence.createUpdateOperations(DelegateScope.class);
    setUnset(updateOperations, DelegateScopeKeys.name, delegateScope.getName());
    setUnset(updateOperations, DelegateScopeKeys.taskTypes, delegateScope.getTaskTypes());
    setUnset(updateOperations, DelegateScopeKeys.environmentTypes, delegateScope.getEnvironmentTypes());
    setUnset(updateOperations, DelegateScopeKeys.applications, delegateScope.getApplications());
    setUnset(updateOperations, DelegateScopeKeys.environments, delegateScope.getEnvironments());
    setUnset(updateOperations, DelegateScopeKeys.serviceInfrastructures, delegateScope.getServiceInfrastructures());
    setUnset(
        updateOperations, DelegateScopeKeys.infrastructureDefinitions, delegateScope.getInfrastructureDefinitions());
    setUnset(updateOperations, DelegateScopeKeys.services, delegateScope.getServices());

    Query<DelegateScope> query = wingsPersistence.createQuery(DelegateScope.class)
                                     .filter(DelegateScopeKeys.accountId, delegateScope.getAccountId())
                                     .filter(ID_KEY, delegateScope.getUuid());
    wingsPersistence.update(query, updateOperations);
    DelegateScope updatedDelegateScope = get(delegateScope.getAccountId(), delegateScope.getUuid());
    log.info("Updated delegate scope: {}", updatedDelegateScope.getUuid());
    auditServiceHelper.reportForAuditingUsingAccountId(
        updatedDelegateScope.getAccountId(), null, updatedDelegateScope, Event.Type.UPDATE);
    log.info(
        "Auditing updating of DelegateScope={} for account={}", delegateScope.getUuid(), delegateScope.getAccountId());

    List<Delegate> delegates = wingsPersistence.createQuery(Delegate.class)
                                   .filter(DelegateKeys.accountId, updatedDelegateScope.getAccountId())
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
    if (!delegateScope.isValid()) {
      log.warn("Delegate scope cannot be empty.");
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Delegate scope cannot be empty.");
    }

    try {
      wingsPersistence.save(delegateScope);
    } catch (DuplicateKeyException e) {
      ignoredOnPurpose(e);
      throw new InvalidRequestException("Scope with given name already exists for this account");
    }
    log.info("Added delegate scope: {}", delegateScope.getUuid());
    auditServiceHelper.reportForAuditingUsingAccountId(
        delegateScope.getAccountId(), null, delegateScope, Event.Type.CREATE);
    log.info("Auditing adding of DelegateScope for accountId={}", delegateScope.getAccountId());
    return delegateScope;
  }

  @Override
  public void delete(String accountId, String delegateScopeId) {
    DelegateScope delegateScope = wingsPersistence.createQuery(DelegateScope.class)
                                      .filter(DelegateScopeKeys.accountId, accountId)
                                      .filter(ID_KEY, delegateScopeId)
                                      .get();
    if (delegateScope != null) {
      ensureScopeSafeToDelete(accountId, delegateScope);
      log.info("Deleting delegate scope: {}", delegateScopeId);
      wingsPersistence.delete(delegateScope);
      auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, delegateScope);
      log.info("Auditing deletion of DelegateScope for accountId={}", accountId);
    }
  }

  private void ensureScopeSafeToDelete(String accountId, DelegateScope delegateScope) {
    String delegateScopeId = delegateScope.getUuid();
    List<Delegate> delegates =
        wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.accountId, accountId).asList();
    List<String> delegateNames = delegates.stream()
                                     .filter(delegate
                                         -> (isNotEmpty(delegate.getIncludeScopes())
                                                && delegate.getIncludeScopes().stream().anyMatch(
                                                    scope -> scope.getUuid().equals(delegateScopeId)))
                                             || (isNotEmpty(delegate.getExcludeScopes())
                                                 && delegate.getExcludeScopes().stream().anyMatch(
                                                     scope -> scope.getUuid().equals(delegateScopeId))))
                                     .map(Delegate::getHostName)
                                     .collect(toList());
    if (isNotEmpty(delegateNames)) {
      String message = format("Delegate scope [%s] could not be deleted because it's used by these delegates [%s]",
          delegateScope.getName(), Joiner.on(", ").join(delegateNames));
      throw new InvalidRequestException(message, USER);
    }
  }
}
