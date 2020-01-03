package software.wings.service.impl;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureFlag.FeatureFlagKeys;
import software.wings.beans.FeatureName;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.intfc.FeatureFlagService;

public class FeatureFlagServiceImplIntegrationTest extends BaseIntegrationTest {
  private static final String ACCOUNT_ID1 = "ACCOUNT_ID1";
  private static final String ACCOUNT_ID2 = "ACCOUNT_ID2";
  @Inject FeatureFlagService featureFlagService;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(IntegrationTests.class)
  public void testEnableAccount() {
    shouldEnableWhenFeatureFlagNotAlreadyPresent();
    shouldEnableWhenSomeAccountsPresent();
  }

  private void shouldEnableWhenFeatureFlagNotAlreadyPresent() {
    FeatureFlag featureFlag = wingsPersistence.createQuery(FeatureFlag.class)
                                  .filter(FeatureFlagKeys.name, FeatureName.INFRA_MAPPING_REFACTOR.name())
                                  .get();
    if (featureFlag != null) {
      wingsPersistence.delete(FeatureFlag.class, featureFlag.getUuid());
    }

    featureFlagService.enableAccount(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID1);

    assertThat(featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID1)).isTrue();
  }

  private void shouldEnableWhenSomeAccountsPresent() {
    assertThat(featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID1)).isTrue();
    assertThat(featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID2)).isFalse();

    featureFlagService.enableAccount(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID2);

    assertThat(featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID2)).isTrue();
  }
}