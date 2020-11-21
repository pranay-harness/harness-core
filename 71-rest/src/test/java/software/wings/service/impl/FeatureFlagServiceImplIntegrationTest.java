package software.wings.service.impl;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;

import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureFlag.FeatureFlagKeys;
import software.wings.beans.FeatureName;
import software.wings.integration.IntegrationTestBase;
import software.wings.service.intfc.FeatureFlagService;

import com.google.inject.Inject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FeatureFlagServiceImplIntegrationTest extends IntegrationTestBase {
  private static final String ACCOUNT_ID1 = "ACCOUNT_ID1";
  private static final String ACCOUNT_ID2 = "ACCOUNT_ID2";
  @Inject FeatureFlagService featureFlagService;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testEnableAccount() {
    shouldEnableWhenFeatureFlagNotAlreadyPresent();
    shouldEnableWhenSomeAccountsPresent();
  }

  private void shouldEnableWhenFeatureFlagNotAlreadyPresent() {
    FeatureFlag featureFlag = wingsPersistence.createQuery(FeatureFlag.class)
                                  .filter(FeatureFlagKeys.name, FeatureName.INLINE_SSH_COMMAND.name())
                                  .get();
    if (featureFlag != null) {
      wingsPersistence.delete(FeatureFlag.class, featureFlag.getUuid());
    }

    featureFlagService.enableAccount(FeatureName.INLINE_SSH_COMMAND, ACCOUNT_ID1);

    assertThat(featureFlagService.isEnabled(FeatureName.INLINE_SSH_COMMAND, ACCOUNT_ID1)).isTrue();
  }

  private void shouldEnableWhenSomeAccountsPresent() {
    assertThat(featureFlagService.isEnabled(FeatureName.INLINE_SSH_COMMAND, ACCOUNT_ID1)).isTrue();
    assertThat(featureFlagService.isEnabled(FeatureName.INLINE_SSH_COMMAND, ACCOUNT_ID2)).isFalse();

    featureFlagService.enableAccount(FeatureName.INLINE_SSH_COMMAND, ACCOUNT_ID2);

    assertThat(featureFlagService.isEnabled(FeatureName.INLINE_SSH_COMMAND, ACCOUNT_ID2)).isTrue();
  }
}
