package io.harness.generator;

import static io.harness.govern.Switch.unhandled;

import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditSource;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Account;
import software.wings.beans.HttpMethod;
import software.wings.service.intfc.AuditService;

import java.util.Arrays;
import java.util.LinkedList;

public class ChangeSetGenerator {
  @Inject AccountGenerator accountGenerator;
  @Inject AuditService auditService;

  public AuditHeader ensurePredefined(Randomizer.Seed seed, OwnerManager.Owners owners, AuditSource source) {
    switch (source) {
      case USER:
        return ensureUserChangeSetTest(seed, owners);
      case GIT:
        return ensureGitChangeSetTest(seed, owners);
      default:
        unhandled(source);
    }
    return null;
  }

  private AuditHeader buildCommonChangeSet(AuditHeader auditHeader) {
    auditHeader.setRemoteIpAddress("remoteIpAddress");
    auditHeader.setRequestMethod(HttpMethod.POST);
    auditHeader.setResourcePath("resourcePath");
    auditHeader.setUrl("url");
    auditHeader.setResponseStatusCode(200);

    EntityAuditRecord auditRecord = EntityAuditRecord.builder()
                                        .appId("appId")
                                        .appName("appName")
                                        .createdAt(System.currentTimeMillis())
                                        .failure(false)
                                        .operationType("operationType")
                                        .affectedResourceId("affectedResourceId")
                                        .affectedResourceName("affectedResourceName")
                                        .affectedResourceOperation("affectedResourceOperation")
                                        .affectedResourceType("affectedResourceType")
                                        .entityId("entityId")
                                        .entityName("entityName")
                                        .entityType("entityName")
                                        .entityNewYamlRecordId("oldYamlId")
                                        .entityOldYamlRecordId("newYamlId")
                                        .build();

    auditHeader.setEntityAuditRecords(new LinkedList<>(Arrays.asList(auditRecord)));
    return auditHeader;
  }

  public AuditHeader ensureUserChangeSetTest(Randomizer.Seed seed, OwnerManager.Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    AuditHeader auditHeader = new AuditHeader();
    auditHeader.setUuid("changeSetId");
    auditHeader.setCreatedBy(owners.obtainUser());
    auditHeader.setAccountId(account.getUuid());

    return ensureChangeSet(buildCommonChangeSet(auditHeader));
  }

  public AuditHeader ensureGitChangeSetTest(Randomizer.Seed seed, OwnerManager.Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    AuditHeader auditHeader = new AuditHeader();
    auditHeader.setUuid("changeSetId");
    auditHeader.setCreatedBy(getGitUser());
    auditHeader.setAccountId(account.getUuid());

    return ensureChangeSet(buildCommonChangeSet(auditHeader));
  }

  private AuditHeader ensureChangeSet(AuditHeader auditHeader) {
    return GeneratorUtils.suppressDuplicateException(
        () -> auditService.create(auditHeader), () -> auditService.create(auditHeader));
  }

  private EmbeddedUser getGitUser() {
    return EmbeddedUser.builder().uuid("GIT").name("GIT_SYNC").build();
  }
}
