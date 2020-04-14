package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Delegate.DelegateBuilder;
import static software.wings.beans.Delegate.Status;
import static software.wings.beans.DelegateProfile.DelegateProfileBuilder;
import static software.wings.beans.DelegateProfile.DelegateProfileKeys;
import static software.wings.service.impl.DelegateProfileServiceImpl.PRIMARY_PROFILE_DESCRIPTION;
import static software.wings.service.impl.DelegateProfileServiceImpl.PRIMARY_PROFILE_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateProfile;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.DelegateProfileServiceImpl;

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
}
