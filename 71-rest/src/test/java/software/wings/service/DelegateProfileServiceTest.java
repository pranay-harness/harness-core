package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.NIKOLA;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;

import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Delegate.DelegateBuilder;
import static software.wings.beans.Delegate.Status;
import static software.wings.service.impl.DelegateProfileServiceImpl.PRIMARY_PROFILE_DESCRIPTION;
import static software.wings.service.impl.DelegateProfileServiceImpl.PRIMARY_PROFILE_NAME;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfile.DelegateProfileBuilder;
import io.harness.delegate.beans.DelegateProfile.DelegateProfileKeys;
import io.harness.delegate.beans.DelegateProfileScopingRule;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.DelegateProfileServiceImpl;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DelegateProfileServiceTest extends WingsBaseTest {
  public static final String ACCOUNT_ID = generateUuid();
  public static final String DELEGATE_PROFILE_NAME = "DELEGATE_PROFILE_NAME";
  public static final String DELEGATE_PROFILE_DESC = "DELEGATE_PROFILE_DESC";
  public static final String VERSION = "1.0.0";
  public static final String IP = "127.0.0.1";
  public static final String HOSTNAME = "delegate";
  public static final String COMPANY_NAME = "COMPANY_NAME";
  public static final String ACCOUNT_NAME = "ACCOUNT_NAME";
  public static final String ACCOUNT_KEY = "ACCOUNT_KEY";

  @Mock private AuditServiceHelper auditServiceHelper;
  @InjectMocks @Inject private DelegateProfileServiceImpl delegateProfileService;

  private DelegateProfileBuilder createDelegateProfileBuilder() {
    return DelegateProfile.builder()
        .accountId(ACCOUNT_ID)
        .name(DELEGATE_PROFILE_NAME)
        .description(DELEGATE_PROFILE_DESC);
  }

  private DelegateBuilder createDelegateBuilder() {
    return Delegate.builder()
        .accountId(ACCOUNT_ID)
        .ip(IP)
        .hostName(HOSTNAME)
        .version(VERSION)
        .status(Status.ENABLED)
        .lastHeartBeat(System.currentTimeMillis());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetShouldFetchFromDb() {
    String accountId = generateUuid();
    String uuid = generateUuid();
    DelegateProfile delegateProfile = createDelegateProfileBuilder().accountId(accountId).uuid(uuid).build();
    wingsPersistence.save(delegateProfile);

    DelegateProfile fetchedProfile = delegateProfileService.get(accountId, uuid);
    assertThat(delegateProfile).isEqualTo(fetchedProfile);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetShouldReturnNull() {
    DelegateProfile fetchedProfile = delegateProfileService.get(generateUuid(), null);
    assertThat(fetchedProfile).isNull();

    fetchedProfile = delegateProfileService.get(generateUuid(), "");
    assertThat(fetchedProfile).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnAccountCreatedShouldCreatePrimaryProfile() {
    Account account = anAccount()
                          .withUuid(generateUuid())
                          .withCompanyName(COMPANY_NAME)
                          .withAccountName(ACCOUNT_NAME)
                          .withAccountKey(ACCOUNT_KEY)
                          .withLicenseInfo(getLicenseInfo())
                          .build();

    delegateProfileService.onAccountCreated(account);

    DelegateProfile primaryProfile = wingsPersistence.createQuery(DelegateProfile.class)
                                         .field(DelegateProfileKeys.accountId)
                                         .equal(account.getUuid())
                                         .field(DelegateProfileKeys.primary)
                                         .equal(true)
                                         .get();

    assertThat(primaryProfile).isNotNull();
    assertThat(primaryProfile.getUuid()).isNotNull();
    assertThat(primaryProfile.getAccountId()).isEqualTo(account.getUuid());
    assertThat(primaryProfile.getName()).isEqualTo(PRIMARY_PROFILE_NAME);
    assertThat(primaryProfile.getDescription()).isEqualTo(PRIMARY_PROFILE_DESCRIPTION);
    assertThat(primaryProfile.isPrimary()).isEqualTo(true);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnAccountCreatedShouldNotCreatePrimaryProfile() {
    Account account = anAccount()
                          .withUuid(generateUuid())
                          .withCompanyName(COMPANY_NAME)
                          .withAccountName(ACCOUNT_NAME)
                          .withAccountKey(ACCOUNT_KEY)
                          .withLicenseInfo(getLicenseInfo())
                          .build();

    account.setForImport(true);

    delegateProfileService.onAccountCreated(account);

    DelegateProfile primaryProfile = wingsPersistence.createQuery(DelegateProfile.class)
                                         .field(DelegateProfileKeys.accountId)
                                         .equal(account.getUuid())
                                         .field(DelegateProfileKeys.primary)
                                         .equal(true)
                                         .get();

    assertThat(primaryProfile).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testFetchPrimaryProfileShouldFetchFromDb() {
    String accountId = "existingAccountId";

    DelegateProfile primaryProfile = DelegateProfile.builder()
                                         .uuid(generateUuid())
                                         .accountId(accountId)
                                         .name(PRIMARY_PROFILE_NAME)
                                         .description(PRIMARY_PROFILE_DESCRIPTION)
                                         .primary(true)
                                         .build();

    wingsPersistence.save(primaryProfile);

    DelegateProfile fetchedProfile = delegateProfileService.fetchPrimaryProfile(accountId);

    assertThat(fetchedProfile).isNotNull();
    assertThat(fetchedProfile.getUuid()).isNotNull();
    assertThat(fetchedProfile.getAccountId()).isEqualTo(accountId);
    assertThat(fetchedProfile.getName()).isEqualTo(PRIMARY_PROFILE_NAME);
    assertThat(fetchedProfile.getDescription()).isEqualTo(PRIMARY_PROFILE_DESCRIPTION);
    assertThat(fetchedProfile.isPrimary()).isEqualTo(true);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testFetchPrimaryProfileShouldCreateProfile() {
    String accountId = "nonExistingAccountId";

    DelegateProfile fetchedProfile = delegateProfileService.fetchPrimaryProfile(accountId);

    assertThat(fetchedProfile).isNotNull();
    assertThat(fetchedProfile.getUuid()).isNotNull();
    assertThat(fetchedProfile.getAccountId()).isEqualTo(accountId);
    assertThat(fetchedProfile.getName()).isEqualTo(PRIMARY_PROFILE_NAME);
    assertThat(fetchedProfile.getDescription()).isEqualTo(PRIMARY_PROFILE_DESCRIPTION);
    assertThat(fetchedProfile.isPrimary()).isEqualTo(true);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDeleteWithNonExistingProfileShouldExitWithoutAction() {
    delegateProfileService.delete(ACCOUNT_ID, generateUuid());

    verify(auditServiceHelper, never())
        .reportDeleteForAuditingUsingAccountId(eq(ACCOUNT_ID), any(DelegateProfile.class));
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDeleteWithAssignedProfileShouldThrowInvalidRequestException() {
    DelegateProfileBuilder delegateProfileBuilder = createDelegateProfileBuilder();
    DelegateProfile assignedDelegateProfile = delegateProfileBuilder.uuid(generateUuid()).build();
    wingsPersistence.save(assignedDelegateProfile);

    DelegateBuilder delegateBuilder = createDelegateBuilder();
    Delegate delegate = delegateBuilder.delegateProfileId(assignedDelegateProfile.getUuid()).build();
    wingsPersistence.save(delegate);

    delegateProfileService.delete(ACCOUNT_ID, assignedDelegateProfile.getUuid());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDeleteWithNonAssignedProfileShouldDeleteProfile() {
    DelegateProfileBuilder delegateProfileBuilder = createDelegateProfileBuilder();
    DelegateProfile nonAssignedDelegateProfile = delegateProfileBuilder.uuid(generateUuid()).build();
    wingsPersistence.save(nonAssignedDelegateProfile);

    delegateProfileService.delete(ACCOUNT_ID, nonAssignedDelegateProfile.getUuid());

    assertThat(wingsPersistence.get(DelegateProfile.class, nonAssignedDelegateProfile.getUuid())).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testUpdateShouldUpdateProfile() {
    // String uuid = generateUuid();
    String updatedName = "updatedName";
    String updatedDescription = "updatedDescription";
    String updatedScript = "updatedScript";
    List<String> profileSelectors = Arrays.asList("selector1", "selector2");

    DelegateProfile delegateProfile =
        createDelegateProfileBuilder() /*.uuid(uuid)*/.startupScript("script").approvalRequired(false).build();
    DelegateProfileScopingRule rule = DelegateProfileScopingRule.builder().description("test").build();
    delegateProfile.setScopingRules(asList(rule));
    wingsPersistence.save(delegateProfile);

    delegateProfile.setName(updatedName);
    delegateProfile.setDescription(updatedDescription);
    delegateProfile.setStartupScript(updatedScript);
    delegateProfile.setApprovalRequired(true);
    delegateProfile.setSelectors(profileSelectors);

    DelegateProfile updatedDelegateProfile = delegateProfileService.update(delegateProfile);

    assertThat(updatedDelegateProfile.getName()).isEqualTo(updatedName);
    assertThat(updatedDelegateProfile.getDescription()).isEqualTo(updatedDescription);
    assertThat(updatedDelegateProfile.getStartupScript()).isEqualTo(updatedScript);
    assertThat(updatedDelegateProfile.isApprovalRequired()).isEqualTo(true);
    assertThat(updatedDelegateProfile.getSelectors()).isEqualTo(profileSelectors);
    assertThat(updatedDelegateProfile.getScopingRules()).containsExactly(rule);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testShouldAddProfile() {
    String profileName = "testProfileName";
    String profileDescription = "tesProfileDescription";
    String accountId = generateUuid();
    List<String> profileSelectors = Arrays.asList("testSelector1", "testSelector2");

    DelegateProfile delegateProfile = createDelegateProfileBuilder()
                                          .accountId(accountId)
                                          .name(profileName)
                                          .description(profileDescription)
                                          .startupScript("script")
                                          .selectors(profileSelectors)
                                          .build();

    wingsPersistence.save(delegateProfile);

    DelegateProfile savedDelegateProfile = delegateProfileService.add(delegateProfile);

    assertThat(savedDelegateProfile).isNotNull();
    assertThat(savedDelegateProfile.getAccountId()).isEqualTo(accountId);
    assertThat(savedDelegateProfile.getName()).isEqualTo(profileName);
    assertThat(savedDelegateProfile.getDescription()).isEqualTo(profileDescription);
    assertThat(savedDelegateProfile.getSelectors().size()).isEqualTo(2);
    assertThat(savedDelegateProfile.getSelectors()).isEqualTo(profileSelectors);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testShouldUpdateDelegateProfileSelectors() {
    String uuid = generateUuid();
    String accountId = generateUuid();
    List<String> profileSelectors =
        Arrays.asList("testProfileSelector1", "testProfileSelector2", "testProfileSelector3");

    DelegateProfile delegateProfile = DelegateProfile.builder()
                                          .uuid(uuid)
                                          .accountId(accountId)
                                          .startupScript("script")
                                          .approvalRequired(false)
                                          .selectors(profileSelectors)
                                          .build();

    wingsPersistence.save(delegateProfile);

    List<String> profileSelectorsUpdated =
        Arrays.asList("updatedProfileSelector1", "updatedProfileSelector2", "testProfileSelector3");

    delegateProfileService.updateDelegateProfileSelectors(uuid, accountId, profileSelectorsUpdated);

    DelegateProfile retrievedDelegateProfile = wingsPersistence.createQuery(DelegateProfile.class)
                                                   .filter(DelegateProfileKeys.uuid, delegateProfile.getUuid())
                                                   .get();

    assertThat(retrievedDelegateProfile).isNotNull();
    assertThat(retrievedDelegateProfile.getSelectors()).hasSize(3);
    assertThat(retrievedDelegateProfile.getSelectors())
        .containsExactly("updatedProfileSelector1", "updatedProfileSelector2", "testProfileSelector3");
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testUpdateShouldUpdateScopingRules() {
    DelegateProfile delegateProfile =
        createDelegateProfileBuilder().startupScript("script").approvalRequired(false).build();

    wingsPersistence.save(delegateProfile);

    DelegateProfileScopingRule rule = DelegateProfileScopingRule.builder().description("test").build();
    DelegateProfile updatedDelegateProfile = delegateProfileService.updateScopingRules(
        delegateProfile.getAccountId(), delegateProfile.getUuid(), asList(rule));

    assertThat(updatedDelegateProfile.getScopingRules()).containsExactly(rule);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testUpdateShouldUpdateScopingRulesWithNull() {
    DelegateProfileScopingRule rule = DelegateProfileScopingRule.builder().description("test").build();
    DelegateProfile delegateProfile = createDelegateProfileBuilder()
                                          .startupScript("script")
                                          .scopingRules(asList(rule))
                                          .approvalRequired(false)
                                          .build();
    wingsPersistence.save(delegateProfile);

    DelegateProfile updatedDelegateProfile =
        delegateProfileService.updateScopingRules(delegateProfile.getAccountId(), delegateProfile.getUuid(), null);

    assertThat(updatedDelegateProfile.getScopingRules()).isNull();
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testUpdateShouldUpdateScopingRulesWithEmptyList() {
    DelegateProfileScopingRule rule = DelegateProfileScopingRule.builder().description("test").build();
    DelegateProfile delegateProfile = createDelegateProfileBuilder()
                                          .startupScript("script")
                                          .scopingRules(asList(rule))
                                          .approvalRequired(false)
                                          .build();
    wingsPersistence.save(delegateProfile);

    DelegateProfile updatedDelegateProfile = delegateProfileService.updateScopingRules(
        delegateProfile.getAccountId(), delegateProfile.getUuid(), emptyList());

    assertThat(updatedDelegateProfile.getScopingRules()).isNull();
  }

  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testShouldUpdateDelegateProfileSelectorsWithNull() {
    String uuid = generateUuid();
    String accountId = generateUuid();
    List<String> profileSelectors =
        Arrays.asList("testProfileSelector1", "testProfileSelector2", "testProfileSelector3");

    DelegateProfile delegateProfile = DelegateProfile.builder()
                                          .uuid(uuid)
                                          .accountId(accountId)
                                          .startupScript("script")
                                          .approvalRequired(false)
                                          .selectors(profileSelectors)
                                          .build();

    wingsPersistence.save(delegateProfile);

    delegateProfileService.updateDelegateProfileSelectors(uuid, accountId, null);

    DelegateProfile retrievedDelegateProfile = wingsPersistence.createQuery(DelegateProfile.class)
                                                   .filter(DelegateProfileKeys.uuid, delegateProfile.getUuid())
                                                   .get();

    assertThat(retrievedDelegateProfile).isNotNull();
    assertThat(retrievedDelegateProfile.getSelectors()).isNullOrEmpty();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testShouldNotUpdateDelegateProfileSelectorsWithEmptyList() {
    String uuid = generateUuid();
    String accountId = generateUuid();
    List<String> profileSelectors =
        Arrays.asList("testProfileSelector1", "testProfileSelector2", "testProfileSelector3");

    DelegateProfile delegateProfile = DelegateProfile.builder()
                                          .uuid(uuid)
                                          .accountId(accountId)
                                          .startupScript("script")
                                          .approvalRequired(false)
                                          .selectors(profileSelectors)
                                          .build();

    wingsPersistence.save(delegateProfile);

    List<String> profileSelectorsUpdated = emptyList();

    delegateProfileService.updateDelegateProfileSelectors(uuid, accountId, profileSelectorsUpdated);

    DelegateProfile retrievedDelegateProfile = wingsPersistence.createQuery(DelegateProfile.class)
                                                   .filter(DelegateProfileKeys.uuid, delegateProfile.getUuid())
                                                   .get();

    assertThat(retrievedDelegateProfile).isNotNull();
    assertThat(retrievedDelegateProfile.getSelectors()).isNullOrEmpty();
  }

  @Test
  @Owner(developers = NIKOLA)
  @Category(UnitTests.class)
  public void shouldDeleteByAccountId() {
    DelegateProfileBuilder delegateProfileBuilder = createDelegateProfileBuilder();
    DelegateProfile assignedDelegateProfile = delegateProfileBuilder.uuid(generateUuid()).build();
    wingsPersistence.save(assignedDelegateProfile);

    delegateProfileService.deleteByAccountId(ACCOUNT_ID);
    assertThat(delegateProfileService.get(ACCOUNT_ID, assignedDelegateProfile.getUuid())).isNull();
  }
}
