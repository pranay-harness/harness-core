package software.wings.service.intfc;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.audit.EntityAuditRecord;
import software.wings.audit.ResourceType;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.ResourceLookup;
import software.wings.beans.ResourceLookup.ResourceLookupKeys;
import software.wings.beans.User;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResourceLookupServiceTest extends WingsBaseTest {
  @InjectMocks @Inject private ResourceLookupService resourceLookupService;
  @Inject private HPersistence persistence;

  private EntityAuditRecord entityAuditRecord;
  private Account account;
  private String entityId;
  private String entityName;
  private String appId;

  @Before
  public void setup() {
    entityId = generateUuid();
    entityName = "name";
    appId = generateUuid();
    entityAuditRecord = EntityAuditRecord.builder()
                            .entityType(ResourceType.USER.name())
                            .entityId(entityId)
                            .entityName(entityName)
                            .appId(appId)
                            .affectedResourceType(ResourceType.USER.name())
                            .affectedResourceId(entityId)
                            .build();
    account = getAccount(AccountType.PAID);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC0_testSaveResourceLookupRecordIfNeeded() {
    resourceLookupService.saveResourceLookupRecordIfNeeded(entityAuditRecord, account.getUuid());
    ResourceLookup resourceLookup =
        persistence.createQuery(ResourceLookup.class)
            .filter(ResourceLookupKeys.accountId, account.getUuid())
            .filter(ResourceLookupKeys.resourceType, entityAuditRecord.getAffectedResourceType())
            .filter(ResourceLookupKeys.resourceId, entityAuditRecord.getAffectedResourceId())
            .disableValidation()
            .get();
    assertThat(resourceLookup).isNotNull();
    persistence.delete(resourceLookup);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC1_testUpdateResourceLookupRecordIfNeeded() {
    User user = new User();
    user.setName("name");

    User updatedUser = user;
    updatedUser.setName("updated_user");

    entityAuditRecord.setEntityName("updated_user");

    resourceLookupService.updateResourceLookupRecordIfNeeded(entityAuditRecord, account.getUuid(), updatedUser, user);

    ResourceLookup resourceLookup =
        persistence.createQuery(ResourceLookup.class)
            .filter(ResourceLookupKeys.accountId, account.getUuid())
            .filter(ResourceLookupKeys.resourceType, entityAuditRecord.getAffectedResourceType())
            .filter(ResourceLookupKeys.resourceId, entityAuditRecord.getAffectedResourceId())
            .disableValidation()
            .get();

    assertThat(resourceLookup).isNotNull();
    assertThat(resourceLookup.getResourceName()).isEqualTo("updated_user");
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC2_testUpdateResourceLookupRecordIfNeeded() {
    User user = new User();
    user.setName("user");

    User updatedUser = user;
    updatedUser.setName("updated_user");

    entityAuditRecord.setEntityName("updated_user");

    ResourceLookup resourceLookupSave = ResourceLookup.builder()
                                            .resourceId(entityId)
                                            .accountId(account.getUuid())
                                            .resourceType(ResourceType.USER.name())
                                            .appId(appId)
                                            .resourceName("user")
                                            .build();
    persistence.save(resourceLookupSave);

    resourceLookupService.updateResourceLookupRecordIfNeeded(entityAuditRecord, account.getUuid(), updatedUser, user);
    ResourceLookup resourceLookup =
        persistence.createQuery(ResourceLookup.class)
            .filter(ResourceLookupKeys.accountId, account.getUuid())
            .filter(ResourceLookupKeys.resourceType, entityAuditRecord.getAffectedResourceType())
            .filter(ResourceLookupKeys.resourceId, entityAuditRecord.getAffectedResourceId())
            .disableValidation()
            .get();

    assertThat(resourceLookup).isNotNull();
    assertThat(resourceLookup.getResourceName()).isEqualTo("updated_user");

    persistence.delete(resourceLookup);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC3_testUpdateResourceName() {
    String uuid = generateUuid();
    ResourceLookup resourceLookup = ResourceLookup.builder()
                                        .uuid(uuid)
                                        .resourceId(entityId)
                                        .resourceName(entityName)
                                        .resourceType(ResourceType.USER.name())
                                        .build();

    persistence.save(resourceLookup);

    resourceLookup.setResourceName("updated_name");

    resourceLookupService.updateResourceName(resourceLookup);

    ResourceLookup updatedResourceLookup = persistence.get(ResourceLookup.class, uuid);

    assertThat(updatedResourceLookup).isNotNull();
    assertThat(updatedResourceLookup.getResourceName()).isEqualTo("updated_name");
    persistence.delete(resourceLookup);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC4_testDelete() {
    String uuid = generateUuid();
    ResourceLookup resourceLookup = ResourceLookup.builder()
                                        .uuid(uuid)
                                        .resourceId(entityId)
                                        .resourceName(entityName)
                                        .resourceType(ResourceType.USER.name())
                                        .build();

    persistence.save(resourceLookup);
    resourceLookupService.delete(resourceLookup);

    ResourceLookup updatedResourceLookup = persistence.get(ResourceLookup.class, uuid);

    assertThat(updatedResourceLookup).isNull();
    persistence.delete(resourceLookup);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC5_testDeleteResourceLookupRecordIfNeeded() {
    String uuid = generateUuid();
    ResourceLookup resourceLookup = ResourceLookup.builder()
                                        .uuid(uuid)
                                        .resourceId(entityId)
                                        .resourceName(entityName)
                                        .resourceType(ResourceType.USER.name())
                                        .build();

    persistence.save(resourceLookup);

    resourceLookupService.deleteResourceLookupRecordIfNeeded(entityAuditRecord, account.getUuid());

    ResourceLookup updatedResourceLookup =
        persistence.createQuery(ResourceLookup.class)
            .filter(ResourceLookupKeys.accountId, account.getUuid())
            .filter(ResourceLookupKeys.resourceId, entityAuditRecord.getAffectedResourceId())
            .get();
    assertThat(updatedResourceLookup).isNull();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC6_testDeleteResourceLookupRecordIfNeeded_NegativeCase() {
    String uuid = generateUuid();
    ResourceLookup resourceLookup = ResourceLookup.builder()
                                        .uuid(uuid)
                                        .resourceId(entityId)
                                        .resourceName(entityName)
                                        .resourceType("negative_resource_type")
                                        .build();
    entityAuditRecord.setEntityType("negative_resource_type");
    persistence.save(resourceLookup);

    resourceLookupService.deleteResourceLookupRecordIfNeeded(entityAuditRecord, account.getUuid());

    ResourceLookup updatedResourceLookup = persistence.get(ResourceLookup.class, uuid);
    assertThat(updatedResourceLookup).isNotNull();

    persistence.delete(resourceLookup);
  }
}
