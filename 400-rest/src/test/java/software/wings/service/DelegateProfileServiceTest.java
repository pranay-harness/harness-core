package software.wings.service;

import static io.harness.beans.FeatureName.PER_AGENT_CAPABILITIES;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.Delegate.DelegateBuilder;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.NICOLAS;
import static io.harness.rule.OwnerRule.NIKOLA;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;

import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.service.impl.DelegateProfileServiceImpl.CG_PRIMARY_PROFILE_NAME;
import static software.wings.service.impl.DelegateProfileServiceImpl.NG_PRIMARY_PROFILE_NAME;
import static software.wings.service.impl.DelegateProfileServiceImpl.PRIMARY_PROFILE_DESCRIPTION;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfile.DelegateProfileBuilder;
import io.harness.delegate.beans.DelegateProfile.DelegateProfileKeys;
import io.harness.delegate.beans.DelegateProfileScopingRule;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.observer.Subject;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.service.intfc.DelegateProfileObserver;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.DelegateProfileServiceImpl;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@BreakDependencyOn("software.wings.WingsBaseTest")
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
  public static final String TEST_IDENTIFIER = "testIdentifier";

  @Mock private Subject<DelegateProfileObserver> delegateProfileSubject;
  @Mock private AuditServiceHelper auditServiceHelper;
  @Mock private FeatureFlagService featureFlagService;
  @InjectMocks @Inject private DelegateProfileServiceImpl delegateProfileService;
  @Inject private HPersistence persistence;

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
        .status(DelegateInstanceStatus.ENABLED)
        .lastHeartBeat(System.currentTimeMillis());
  }

  @Before
  public void setup() throws IllegalAccessException {
    initMocks(this);
    FieldUtils.writeField(delegateProfileService, "delegateProfileSubject", delegateProfileSubject, true);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetShouldFetchFromDb() {
    String accountId = generateUuid();
    String uuid = generateUuid();
    DelegateProfile delegateProfile = createDelegateProfileBuilder().accountId(accountId).uuid(uuid).build();
    persistence.save(delegateProfile);

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

    DelegateProfile cgPrimaryProfile = persistence.createQuery(DelegateProfile.class)
                                           .field(DelegateProfileKeys.accountId)
                                           .equal(account.getUuid())
                                           .field(DelegateProfileKeys.primary)
                                           .equal(true)
                                           .filter(DelegateProfileKeys.ng, false)
                                           .get();

    assertThat(cgPrimaryProfile).isNotNull();
    assertThat(cgPrimaryProfile.getUuid()).isNotNull();
    assertThat(cgPrimaryProfile.getAccountId()).isEqualTo(account.getUuid());
    assertThat(cgPrimaryProfile.getName()).isEqualTo(CG_PRIMARY_PROFILE_NAME);
    assertThat(cgPrimaryProfile.getDescription()).isEqualTo(PRIMARY_PROFILE_DESCRIPTION);
    assertThat(cgPrimaryProfile.isPrimary()).isTrue();
    assertThat(cgPrimaryProfile.isNg()).isFalse();

    DelegateProfile ngPrimaryProfile = persistence.createQuery(DelegateProfile.class)
                                           .field(DelegateProfileKeys.accountId)
                                           .equal(account.getUuid())
                                           .field(DelegateProfileKeys.primary)
                                           .equal(true)
                                           .filter(DelegateProfileKeys.ng, true)
                                           .get();

    assertThat(ngPrimaryProfile).isNotNull();
    assertThat(ngPrimaryProfile.getUuid()).isNotNull();
    assertThat(ngPrimaryProfile.getAccountId()).isEqualTo(account.getUuid());
    assertThat(ngPrimaryProfile.getName()).isEqualTo(NG_PRIMARY_PROFILE_NAME);
    assertThat(ngPrimaryProfile.getDescription()).isEqualTo(PRIMARY_PROFILE_DESCRIPTION);
    assertThat(ngPrimaryProfile.isPrimary()).isTrue();
    assertThat(ngPrimaryProfile.isNg()).isTrue();
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

    DelegateProfile primaryProfile = persistence.createQuery(DelegateProfile.class)
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
  public void testFetchCgPrimaryProfileShouldFetchFromDb() {
    String accountId = "existingAccountId";

    DelegateProfile primaryProfile = DelegateProfile.builder()
                                         .uuid(generateUuid())
                                         .accountId(accountId)
                                         .name(CG_PRIMARY_PROFILE_NAME)
                                         .description(PRIMARY_PROFILE_DESCRIPTION)
                                         .primary(true)
                                         .ng(false)
                                         .build();

    persistence.save(primaryProfile);

    DelegateProfile fetchedProfile = delegateProfileService.fetchCgPrimaryProfile(accountId);

    assertThat(fetchedProfile).isNotNull();
    assertThat(fetchedProfile.getUuid()).isNotNull();
    assertThat(fetchedProfile.getAccountId()).isEqualTo(accountId);
    assertThat(fetchedProfile.getName()).isEqualTo(CG_PRIMARY_PROFILE_NAME);
    assertThat(fetchedProfile.getDescription()).isEqualTo(PRIMARY_PROFILE_DESCRIPTION);
    assertThat(fetchedProfile.isPrimary()).isTrue();
    assertThat(fetchedProfile.isNg()).isFalse();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testFetchCgPrimaryProfileShouldCreateProfile() {
    String accountId = "nonExistingAccountId";

    DelegateProfile fetchedProfile = delegateProfileService.fetchCgPrimaryProfile(accountId);

    assertThat(fetchedProfile).isNotNull();
    assertThat(fetchedProfile.getUuid()).isNotNull();
    assertThat(fetchedProfile.getAccountId()).isEqualTo(accountId);
    assertThat(fetchedProfile.getName()).isEqualTo(CG_PRIMARY_PROFILE_NAME);
    assertThat(fetchedProfile.getDescription()).isEqualTo(PRIMARY_PROFILE_DESCRIPTION);
    assertThat(fetchedProfile.isPrimary()).isTrue();
    assertThat(fetchedProfile.isNg()).isFalse();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testFetchNgPrimaryProfileShouldFetchFromDb() {
    String accountId = "existingAccountId";

    DelegateProfile primaryProfile = DelegateProfile.builder()
                                         .uuid(generateUuid())
                                         .accountId(accountId)
                                         .name(NG_PRIMARY_PROFILE_NAME)
                                         .description(PRIMARY_PROFILE_DESCRIPTION)
                                         .primary(true)
                                         .ng(true)
                                         .build();

    persistence.save(primaryProfile);

    DelegateProfile fetchedProfile = delegateProfileService.fetchNgPrimaryProfile(accountId);

    assertThat(fetchedProfile).isNotNull();
    assertThat(fetchedProfile.getUuid()).isNotNull();
    assertThat(fetchedProfile.getAccountId()).isEqualTo(accountId);
    assertThat(fetchedProfile.getName()).isEqualTo(NG_PRIMARY_PROFILE_NAME);
    assertThat(fetchedProfile.getDescription()).isEqualTo(PRIMARY_PROFILE_DESCRIPTION);
    assertThat(fetchedProfile.isPrimary()).isTrue();
    assertThat(fetchedProfile.isNg()).isTrue();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testFetchNgPrimaryProfileShouldCreateProfile() {
    String accountId = "nonExistingAccountId";

    DelegateProfile fetchedProfile = delegateProfileService.fetchNgPrimaryProfile(accountId);

    assertThat(fetchedProfile).isNotNull();
    assertThat(fetchedProfile.getUuid()).isNotNull();
    assertThat(fetchedProfile.getAccountId()).isEqualTo(accountId);
    assertThat(fetchedProfile.getName()).isEqualTo(NG_PRIMARY_PROFILE_NAME);
    assertThat(fetchedProfile.getDescription()).isEqualTo(PRIMARY_PROFILE_DESCRIPTION);
    assertThat(fetchedProfile.isPrimary()).isTrue();
    assertThat(fetchedProfile.isNg()).isTrue();
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
    persistence.save(assignedDelegateProfile);

    DelegateBuilder delegateBuilder = createDelegateBuilder();
    Delegate delegate = delegateBuilder.delegateProfileId(assignedDelegateProfile.getUuid()).build();
    persistence.save(delegate);

    delegateProfileService.delete(ACCOUNT_ID, assignedDelegateProfile.getUuid());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDeleteWithNonAssignedProfileShouldDeleteProfile() {
    DelegateProfileBuilder delegateProfileBuilder = createDelegateProfileBuilder();
    DelegateProfile nonAssignedDelegateProfile = delegateProfileBuilder.uuid(generateUuid()).build();
    persistence.save(nonAssignedDelegateProfile);

    delegateProfileService.delete(ACCOUNT_ID, nonAssignedDelegateProfile.getUuid());

    assertThat(persistence.get(DelegateProfile.class, nonAssignedDelegateProfile.getUuid())).isNull();
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
    delegateProfile.setScopingRules(Collections.singletonList(rule));
    persistence.save(delegateProfile);

    delegateProfile.setName(updatedName);
    delegateProfile.setDescription(updatedDescription);
    delegateProfile.setStartupScript(updatedScript);
    delegateProfile.setApprovalRequired(true);
    delegateProfile.setSelectors(profileSelectors);

    DelegateProfile updatedDelegateProfile = delegateProfileService.update(delegateProfile);

    assertThat(updatedDelegateProfile.getName()).isEqualTo(updatedName);
    assertThat(updatedDelegateProfile.getDescription()).isEqualTo(updatedDescription);
    assertThat(updatedDelegateProfile.getStartupScript()).isEqualTo(updatedScript);
    assertThat(updatedDelegateProfile.isApprovalRequired()).isTrue();
    assertThat(updatedDelegateProfile.getSelectors()).isEqualTo(profileSelectors);
    assertThat(updatedDelegateProfile.getScopingRules()).containsExactly(rule);

    verify(delegateProfileSubject).fireInform(any(), any(DelegateProfile.class), any(DelegateProfile.class));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testShouldAddProfile() {
    String profileName = "testProfileName";
    String profileDescription = "testProfileDescription";
    String accountId = generateUuid();
    List<String> profileSelectors = Arrays.asList("testSelector1", "testSelector2");

    DelegateProfile delegateProfile = createDelegateProfileBuilder()
                                          .accountId(accountId)
                                          .name(profileName)
                                          .description(profileDescription)
                                          .startupScript("script")
                                          .selectors(profileSelectors)
                                          .build();

    DelegateProfile savedDelegateProfile = delegateProfileService.add(delegateProfile);

    assertThat(savedDelegateProfile).isNotNull();
    assertThat(savedDelegateProfile.getAccountId()).isEqualTo(accountId);
    assertThat(savedDelegateProfile.getName()).isEqualTo(profileName);
    assertThat(savedDelegateProfile.getDescription()).isEqualTo(profileDescription);
    assertThat(savedDelegateProfile.getSelectors().size()).isEqualTo(2);
    assertThat(savedDelegateProfile.getSelectors()).isEqualTo(profileSelectors);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testShouldAddProfileWithIdentifier() {
    String profileName = "testProfileName";
    String profileDescription = "testProfileDescription";
    String profileIdentifier = "testProfileIdentifier";
    String accountId = generateUuid();
    List<String> profileSelectors = Arrays.asList("testSelector1", "testSelector2");

    DelegateProfile delegateProfile = createDelegateProfileBuilder()
                                          .accountId(accountId)
                                          .name(profileName)
                                          .description(profileDescription)
                                          .startupScript("script")
                                          .selectors(profileSelectors)
                                          .identifier(profileIdentifier)
                                          .build();

    DelegateProfile savedDelegateProfile = delegateProfileService.add(delegateProfile);

    assertThat(savedDelegateProfile).isNotNull();
    assertThat(savedDelegateProfile.getAccountId()).isEqualTo(accountId);
    assertThat(savedDelegateProfile.getName()).isEqualTo(profileName);
    assertThat(savedDelegateProfile.getDescription()).isEqualTo(profileDescription);
    assertThat(savedDelegateProfile.getSelectors().size()).isEqualTo(2);
    assertThat(savedDelegateProfile.getSelectors()).isEqualTo(profileSelectors);
    assertThat(savedDelegateProfile.getIdentifier()).isEqualTo(profileIdentifier);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testShouldUpdateDelegateProfileSelectors() {
    String uuid = generateUuid();
    String accountId = generateUuid();
    List<String> profileSelectors =
        Arrays.asList("testProfileSelector1", "testProfileSelector2", "testProfileSelector3");

    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, accountId)).thenReturn(true);

    DelegateProfile delegateProfile = DelegateProfile.builder()
                                          .uuid(uuid)
                                          .accountId(accountId)
                                          .startupScript("script")
                                          .approvalRequired(false)
                                          .selectors(profileSelectors)
                                          .build();

    persistence.save(delegateProfile);

    List<String> profileSelectorsUpdated =
        Arrays.asList("updatedProfileSelector1", "updatedProfileSelector2", "testProfileSelector3");

    delegateProfileService.updateDelegateProfileSelectors(uuid, accountId, profileSelectorsUpdated);

    DelegateProfile retrievedDelegateProfile = persistence.createQuery(DelegateProfile.class)
                                                   .filter(DelegateProfileKeys.uuid, delegateProfile.getUuid())
                                                   .get();

    assertThat(retrievedDelegateProfile).isNotNull();
    assertThat(retrievedDelegateProfile.getSelectors()).hasSize(3);
    assertThat(retrievedDelegateProfile.getSelectors())
        .containsExactly("updatedProfileSelector1", "updatedProfileSelector2", "testProfileSelector3");

    verify(delegateProfileSubject).fireInform(any(), eq(accountId), eq(delegateProfile.getUuid()));
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testUpdateShouldUpdateScopingRules() {
    DelegateProfile delegateProfile =
        createDelegateProfileBuilder().startupScript("script").approvalRequired(false).build();

    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegateProfile.getAccountId())).thenReturn(true);

    persistence.save(delegateProfile);

    DelegateProfileScopingRule rule = DelegateProfileScopingRule.builder().description("test").build();
    DelegateProfile updatedDelegateProfile = delegateProfileService.updateScopingRules(
        delegateProfile.getAccountId(), delegateProfile.getUuid(), Collections.singletonList(rule));

    assertThat(updatedDelegateProfile.getScopingRules()).containsExactly(rule);

    verify(delegateProfileSubject).fireInform(any(), eq(delegateProfile.getAccountId()), eq(delegateProfile.getUuid()));
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testUpdateShouldUpdateScopingRulesWithNull() {
    DelegateProfileScopingRule rule = DelegateProfileScopingRule.builder().description("test").build();
    DelegateProfile delegateProfile = createDelegateProfileBuilder()
                                          .startupScript("script")
                                          .scopingRules(Collections.singletonList(rule))
                                          .approvalRequired(false)
                                          .build();
    persistence.save(delegateProfile);

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
                                          .scopingRules(Collections.singletonList(rule))
                                          .approvalRequired(false)
                                          .build();
    persistence.save(delegateProfile);

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

    persistence.save(delegateProfile);

    delegateProfileService.updateDelegateProfileSelectors(uuid, accountId, null);

    DelegateProfile retrievedDelegateProfile = persistence.createQuery(DelegateProfile.class)
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

    persistence.save(delegateProfile);

    List<String> profileSelectorsUpdated = emptyList();

    delegateProfileService.updateDelegateProfileSelectors(uuid, accountId, profileSelectorsUpdated);

    DelegateProfile retrievedDelegateProfile = persistence.createQuery(DelegateProfile.class)
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
    persistence.save(assignedDelegateProfile);

    delegateProfileService.deleteByAccountId(ACCOUNT_ID);
    assertThat(delegateProfileService.get(ACCOUNT_ID, assignedDelegateProfile.getUuid())).isNull();
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void isValidIdentifier_valid() {
    DelegateProfile delegateProfile =
        createDelegateProfileBuilder().uuid(generateUuid()).identifier(TEST_IDENTIFIER).build();

    persistence.save(delegateProfile);

    assertThat(delegateProfileService.isValidIdentifier(ACCOUNT_ID, TEST_IDENTIFIER + "_1")).isTrue();
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void isValidIdentifier_invalid() {
    DelegateProfile delegateProfile =
        createDelegateProfileBuilder().uuid(generateUuid()).identifier(TEST_IDENTIFIER).build();

    persistence.save(delegateProfile);

    assertThat(delegateProfileService.isValidIdentifier(ACCOUNT_ID, TEST_IDENTIFIER)).isFalse();
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void getDelegatesForProfile() {
    String accountId = generateUuid();
    String uuid = generateUuid();
    DelegateProfile delegateProfile = createDelegateProfileBuilder().accountId(accountId).uuid(uuid).build();
    persistence.save(delegateProfile);

    DelegateBuilder delegateBuilder = createDelegateBuilder();
    Delegate delegate = delegateBuilder.delegateProfileId(delegateProfile.getUuid()).accountId(accountId).build();
    persistence.save(delegate);

    List<String> delegatesForProfile =
        delegateProfileService.getDelegatesForProfile(delegateProfile.getAccountId(), delegateProfile.getUuid());

    assertThat(delegatesForProfile).isNotNull();
    assertThat(delegatesForProfile.size()).isEqualTo(1);
  }
}
