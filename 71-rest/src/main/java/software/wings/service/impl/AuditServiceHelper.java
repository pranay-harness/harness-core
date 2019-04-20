package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Event.Type;

import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuditService;

@Singleton
@Slf4j
public class AuditServiceHelper {
  @Inject private AppService appService;
  @Inject private AuditService auditService;

  public void reportDeleteForAuditing(String appId, Object entity) {
    try {
      String accountId = appService.getAccountIdByAppId(appId);
      auditService.registerAuditActions(accountId, entity, null, Type.DELETE);
    } catch (Exception e) {
      logger.warn("Failed to Audit \"Delete\" purge record");
    }
  }

  public void reportForAuditingUsingAppId(String appId, Object oldEntity, Object newEntity, Type type) {
    try {
      String accountId = appService.getAccountIdByAppId(appId);
      auditService.registerAuditActions(accountId, oldEntity, newEntity, type);
    } catch (Exception e) {
      logger.warn("Failed to Audit \"Delete\" purge record");
    }
  }

  public void reportForAuditingUsingAccountId(String accountId, Object oldEntity, Object newEntity, Type type) {
    try {
      auditService.registerAuditActions(accountId, oldEntity, newEntity, type);
    } catch (Exception e) {
      logger.warn("Failed to Audit \"Delete\" purge record");
    }
  }
}
