package software.wings.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.globalcontex.EntityOperationIdentifier;
import io.harness.globalcontex.EntityOperationIdentifier.entityOperation;
import io.harness.manage.GlobalContextManager;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;

public class AuditServiceHelperTest extends WingsBaseTest {
  @Inject AuditServiceHelper auditServiceHelper;

  @Test
  @Category(UnitTests.class)
  public void testAddEntityOperationIdentifierDataToAuditContext_Service() throws Exception {
    try (GlobalContextGuard guard = GlobalContextManager.initGlobalContextGuard(null)) {
      GlobalContextManager.upsertGlobalContextRecord(AuditGlobalContextData.builder().auditId("12345").build());

      auditServiceHelper.addEntityOperationIdentifierDataToAuditContext(EntityOperationIdentifier.builder()
                                                                            .entityId("abc")
                                                                            .entityName("Service1")
                                                                            .entityType(EntityType.SERVICE.name())
                                                                            .operation(entityOperation.CREATE)
                                                                            .build());

      AuditGlobalContextData auditGlobalContextData =
          (AuditGlobalContextData) GlobalContextManager.get(AuditGlobalContextData.AUDIT_ID);
      assertNotNull(auditGlobalContextData);
      assertEquals("12345", auditGlobalContextData.getAuditId());
      assertNotNull(auditGlobalContextData.getEntityOperationIdentifierSet());
      assertEquals(1, auditGlobalContextData.getEntityOperationIdentifierSet().size());
      assertTrue(
          auditGlobalContextData.getEntityOperationIdentifierSet().contains(EntityOperationIdentifier.builder()
                                                                                .entityId("abc")
                                                                                .entityName("Service1")
                                                                                .entityType(EntityType.SERVICE.name())
                                                                                .operation(entityOperation.CREATE)
                                                                                .build()));

      assertFalse(
          auditGlobalContextData.getEntityOperationIdentifierSet().contains(EntityOperationIdentifier.builder()
                                                                                .entityId("abcd") // id changed
                                                                                .entityName("Service1")
                                                                                .entityType(EntityType.SERVICE.name())
                                                                                .operation(entityOperation.CREATE)
                                                                                .build()));

      assertFalse(
          auditGlobalContextData.getEntityOperationIdentifierSet().contains(EntityOperationIdentifier.builder()
                                                                                .entityId("abcd")
                                                                                .entityName("Service10") // name changed
                                                                                .entityType(EntityType.SERVICE.name())
                                                                                .operation(entityOperation.CREATE)
                                                                                .build()));
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testAddEntityOperationIdentifierDataToAuditContext_Environment() throws Exception {
    try (GlobalContextGuard guard = GlobalContextManager.initGlobalContextGuard(null)) {
      GlobalContextManager.upsertGlobalContextRecord(AuditGlobalContextData.builder().auditId("12345").build());

      auditServiceHelper.addEntityOperationIdentifierDataToAuditContext(EntityOperationIdentifier.builder()
                                                                            .entityId("abc")
                                                                            .entityName("Env1")
                                                                            .entityType(EntityType.ENVIRONMENT.name())
                                                                            .operation(entityOperation.CREATE)
                                                                            .build());

      AuditGlobalContextData auditGlobalContextData =
          (AuditGlobalContextData) GlobalContextManager.get(AuditGlobalContextData.AUDIT_ID);
      assertNotNull(auditGlobalContextData);
      assertNotNull(auditGlobalContextData.getEntityOperationIdentifierSet());
      assertEquals("12345", auditGlobalContextData.getAuditId());
      assertEquals(1, auditGlobalContextData.getEntityOperationIdentifierSet().size());
      assertTrue(auditGlobalContextData.getEntityOperationIdentifierSet().contains(
          EntityOperationIdentifier.builder()
              .entityId("abc")
              .entityName("Env1")
              .entityType(EntityType.ENVIRONMENT.name())
              .operation(entityOperation.CREATE)
              .build()));

      assertFalse(auditGlobalContextData.getEntityOperationIdentifierSet().contains(
          EntityOperationIdentifier.builder()
              .entityId("abcd") // id changed
              .entityName("Env1")
              .entityType(EntityType.ENVIRONMENT.name())
              .operation(entityOperation.CREATE)
              .build()));

      assertFalse(auditGlobalContextData.getEntityOperationIdentifierSet().contains(
          EntityOperationIdentifier.builder()
              .entityId("abcd")
              .entityName("Env10") // name changed
              .entityType(EntityType.ENVIRONMENT.name())
              .operation(entityOperation.CREATE)
              .build()));
    }
  }
}
