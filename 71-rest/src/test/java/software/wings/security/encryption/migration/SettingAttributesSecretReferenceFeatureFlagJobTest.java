package software.wings.security.encryption.migration;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.FeatureName.CONNECTORS_REF_SECRETS;
import static software.wings.beans.FeatureName.CONNECTORS_REF_SECRETS_MIGRATION;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.SettingAttribute;

import java.util.concurrent.Future;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class SettingAttributesSecretReferenceFeatureFlagJobTest extends WingsBaseTest {
  @Inject SettingAttributesSecretReferenceFeatureFlagJob settingAttributesSecretReferenceFeatureFlagJob;

  private SettingAttribute createSettingAttribute(Account account) {
    AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(account.getUuid(), null, null);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);
    settingAttribute.setName("testAttribute");
    String settingAttributeId = wingsPersistence.save(settingAttribute);
    settingAttribute = wingsPersistence.get(SettingAttribute.class, settingAttributeId);
    return settingAttribute;
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testStartStop() throws InterruptedException, IllegalAccessException {
    try {
      settingAttributesSecretReferenceFeatureFlagJob.start();
      Future future = (Future) FieldUtils.readField(
          settingAttributesSecretReferenceFeatureFlagJob, "settingAttributeFeatureFlagCheckerFuture", true);
      assertThat(future).isNotNull();
      assertThat(future.isDone()).isFalse();
    } finally {
      settingAttributesSecretReferenceFeatureFlagJob.stop();
      Future future = (Future) FieldUtils.readField(
          settingAttributesSecretReferenceFeatureFlagJob, "settingAttributeFeatureFlagCheckerFuture", true);
      assertThat(future).isNotNull();
      assertThat(future.isCancelled()).isTrue();
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testMigrationWhenMigrationIsGloballyEnabled_shouldNotEnableFeatureFlag() {
    Account account = wingsPersistence.get(Account.class, wingsPersistence.save(getAccount(AccountType.PAID)));
    createSettingAttribute(account);
    featureFlagService.enableGlobally(CONNECTORS_REF_SECRETS_MIGRATION);
    settingAttributesSecretReferenceFeatureFlagJob.run();
    assertThat(featureFlagService.isGlobalEnabled(CONNECTORS_REF_SECRETS)).isFalse();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testMigrationWhenMigrationIsGloballyEnabled_shouldEnableFeatureFlag() {
    Account account = wingsPersistence.get(Account.class, wingsPersistence.save(getAccount(AccountType.PAID)));
    SettingAttribute settingAttribute = createSettingAttribute(account);
    settingAttribute.setSecretsMigrated(true);
    wingsPersistence.save(settingAttribute);
    featureFlagService.enableGlobally(CONNECTORS_REF_SECRETS_MIGRATION);
    settingAttributesSecretReferenceFeatureFlagJob.run();
    assertThat(featureFlagService.isGlobalEnabled(CONNECTORS_REF_SECRETS)).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testMigrationWhenMigrationIsEnabledForMultipleAccounts() {
    Account account1 = wingsPersistence.get(Account.class, wingsPersistence.save(getAccount(AccountType.PAID)));
    Account account2 = wingsPersistence.get(Account.class, wingsPersistence.save(getAccount(AccountType.PAID)));
    Account account3 = wingsPersistence.get(Account.class, wingsPersistence.save(getAccount(AccountType.PAID)));

    featureFlagService.enableAccount(CONNECTORS_REF_SECRETS_MIGRATION, account1.getUuid());
    featureFlagService.enableAccount(CONNECTORS_REF_SECRETS_MIGRATION, account2.getUuid());
    featureFlagService.enableAccount(CONNECTORS_REF_SECRETS_MIGRATION, account3.getUuid());

    SettingAttribute settingAttribute1 = createSettingAttribute(account1);
    settingAttribute1.setSecretsMigrated(true);
    wingsPersistence.save(settingAttribute1);

    SettingAttribute settingAttribute2 = createSettingAttribute(account2);
    settingAttribute2.setSecretsMigrated(false);
    wingsPersistence.save(settingAttribute2);

    createSettingAttribute(account3);

    settingAttributesSecretReferenceFeatureFlagJob.run();

    assertThat(featureFlagService.isEnabled(CONNECTORS_REF_SECRETS, account1.getUuid())).isTrue();
    assertThat(featureFlagService.isEnabled(CONNECTORS_REF_SECRETS, account2.getUuid())).isFalse();
    assertThat(featureFlagService.isEnabled(CONNECTORS_REF_SECRETS, account3.getUuid())).isFalse();
  }
}
