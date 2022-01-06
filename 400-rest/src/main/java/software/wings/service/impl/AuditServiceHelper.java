/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.globalcontex.EntityOperationIdentifier;
import io.harness.manage.GlobalContextManager;
import io.harness.observer.RemoteObserverInformer;
import io.harness.observer.Subject;

import software.wings.beans.Event.Type;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.entitycrud.EntityCrudOperationObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(PL)
@TargetModule(HarnessModule._940_CG_AUDIT_SERVICE)
public class AuditServiceHelper {
  @Inject private AppService appService;
  @Getter private Subject<EntityCrudOperationObserver> entityCrudSubject = new Subject<>();
  @Inject private RemoteObserverInformer remoteObserverInformer;

  public void reportDeleteForAuditing(String appId, Object entity) {
    try {
      String accountId = appService.getAccountIdByAppId(appId);
      entityCrudSubject.fireInform(
          EntityCrudOperationObxxxxxxxx:handleEntityCrudOperation, accountId, entity, null, Type.DELETE);
      // todo(abhinav): object is not kryo serializable.
    } catch (Exception e) {
      log.warn("Failed to Audit \"Delete\" purge record");
    }
  }

  public void reportDeleteForAuditingUsingAccountId(String accountId, Object entity) {
    try {
      entityCrudSubject.fireInform(
          EntityCrudOperationObxxxxxxxx:handleEntityCrudOperation, accountId, entity, null, Type.DELETE);
    } catch (Exception e) {
      log.warn("Failed to Audit \"Delete\" purge record");
    }
  }

  public void reportForAuditingUsingAppId(String appId, Object oldEntity, Object newEntity, Type type) {
    try {
      String accountId = appService.getAccountIdByAppId(appId);
      entityCrudSubject.fireInform(
          EntityCrudOperationObxxxxxxxx:handleEntityCrudOperation, accountId, oldEntity, newEntity, type);
    } catch (Exception e) {
      log.warn("Failed to Audit Record for Type: " + type, e);
    }
  }

  public void reportForAuditingUsingAccountId(String accountId, Object oldEntity, Object newEntity, Type type) {
    try {
      entityCrudSubject.fireInform(
          EntityCrudOperationObxxxxxxxx:handleEntityCrudOperation, accountId, oldEntity, newEntity, type);
    } catch (Exception e) {
      log.warn("Failed to Audit Record for Type: " + type, e);
    }
  }

  public void addEntityOperationIdentifierDataToAuditContext(EntityOperationIdentifier entityOperationIdentifier) {
    AuditGlobalContextData auditGlobalContextData = GlobalContextManager.get(AuditGlobalContextData.AUDIT_ID);
    if (auditGlobalContextData == null) {
      log.warn("auditGlobalContextData Was found Null in addEntityOperationIdentifierDataToAuditContext(): "
          + entityOperationIdentifier.toString());
      return;
    }
    auditGlobalContextData.getEntityOperationIdentifierSet().add(entityOperationIdentifier);
  }
}
