package software.wings.security.encryption.migration;

import static io.harness.rule.OwnerRule.UTKARSH;

import static software.wings.beans.FeatureName.CONNECTORS_REF_SECRETS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.SettingAttribute;

import com.google.inject.Inject;
import java.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
    settingAttributesSecretReferenceFeatureFlagJob.run();
    assertThat(featureFlagService.isGlobalEnabled(CONNECTORS_REF_SECRETS)).isTrue();
  }
}
