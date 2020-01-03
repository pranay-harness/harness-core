package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.globalcontex.EntityOperationIdentifier;
import io.harness.globalcontex.EntityOperationIdentifier.entityOperation;
import io.harness.manage.GlobalContextManager;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;

public class AuditServiceHelperTest extends WingsBaseTest {
  @Inject AuditServiceHelper auditServiceHelper;

  @Test
  @Owner(developers = ADWAIT)
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
      assertThat(auditGlobalContextData).isNotNull();
      assertThat(auditGlobalContextData.getAuditId()).isEqualTo("12345");
      assertThat(auditGlobalContextData.getEntityOperationIdentifierSet()).isNotNull();
      assertThat(auditGlobalContextData.getEntityOperationIdentifierSet()).hasSize(1);
      assertThat(
          auditGlobalContextData.getEntityOperationIdentifierSet().contains(EntityOperationIdentifier.builder()
                                                                                .entityId("abc")
                                                                                .entityName("Service1")
                                                                                .entityType(EntityType.SERVICE.name())
                                                                                .operation(entityOperation.CREATE)
                                                                                .build()))
          .isTrue();

      assertThat(
          auditGlobalContextData.getEntityOperationIdentifierSet().contains(EntityOperationIdentifier.builder()
                                                                                .entityId("abcd") // id changed
                                                                                .entityName("Service1")
                                                                                .entityType(EntityType.SERVICE.name())
                                                                                .operation(entityOperation.CREATE)
                                                                                .build()))
          .isFalse();

      assertThat(
          auditGlobalContextData.getEntityOperationIdentifierSet().contains(EntityOperationIdentifier.builder()
                                                                                .entityId("abcd")
                                                                                .entityName("Service10") // name changed
                                                                                .entityType(EntityType.SERVICE.name())
                                                                                .operation(entityOperation.CREATE)
                                                                                .build()))
          .isFalse();
    }
  }

  @Test
  @Owner(developers = ADWAIT)
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
      assertThat(auditGlobalContextData).isNotNull();
      assertThat(auditGlobalContextData.getEntityOperationIdentifierSet()).isNotNull();
      assertThat(auditGlobalContextData.getAuditId()).isEqualTo("12345");
      assertThat(auditGlobalContextData.getEntityOperationIdentifierSet()).hasSize(1);
      assertThat(auditGlobalContextData.getEntityOperationIdentifierSet().contains(
                     EntityOperationIdentifier.builder()
                         .entityId("abc")
                         .entityName("Env1")
                         .entityType(EntityType.ENVIRONMENT.name())
                         .operation(entityOperation.CREATE)
                         .build()))
          .isTrue();

      assertThat(auditGlobalContextData.getEntityOperationIdentifierSet().contains(
                     EntityOperationIdentifier.builder()
                         .entityId("abcd") // id changed
                         .entityName("Env1")
                         .entityType(EntityType.ENVIRONMENT.name())
                         .operation(entityOperation.CREATE)
                         .build()))
          .isFalse();

      assertThat(auditGlobalContextData.getEntityOperationIdentifierSet().contains(
                     EntityOperationIdentifier.builder()
                         .entityId("abcd")
                         .entityName("Env10") // name changed
                         .entityType(EntityType.ENVIRONMENT.name())
                         .operation(entityOperation.CREATE)
                         .build()))
          .isFalse();
    }
  }
}
