// TODO: See what's right way to proceed with integration tests
// package software.wings.service.intfc;
//
// import static org.assertj.core.api.Assertions.assertThat;
//
// import com.google.inject.Inject;
//
// import io.harness.category.element.IntegrationTests;
// import org.junit.Test;
// import org.junit.experimental.categories.Category;
// import software.wings.beans.FeatureFlag;
// import software.wings.beans.FeatureFlag.FeatureFlagKeys;
// import io.harness.beans.FeatureName;
// import software.wings.integration.IntegrationTestBase;
// import software.wings.utils.WingsTestConstants;
//
// import java.util.Collections;
//
// public class FeatureFlagServiceIntegrationTest extends IntegrationTestBase {
//  @Inject FeatureFlagService featureFlagService;
//
//  @Test
//  @Owner(emails = UNKNOWN)
//  @Category(IntegrationTests.class)
//  public void testEnableAccount() {
//    shouldEnableWhenFeatureFlagNotAlreadyPresent();
//    shouldEnableWhenNullAccountIds();
//    shouldEnableWhenSomeAccountsPresent();
//  }
//
//  private void shouldEnableWhenFeatureFlagNotAlreadyPresent() {
//    ensureDelete();
//
//    featureFlagService.enableAccount(FeatureName.INTEGRATION_TEST, WingsTestConstants.ACCOUNT_ID);
//
//    assertThat(featureFlagService.isEnabled(FeatureName.INTEGRATION_TEST, WingsTestConstants.ACCOUNT_ID)).isTrue();
//  }
//
//  private void ensureDelete() {
//    FeatureFlag featureFlag = wingsPersistence.createQuery(FeatureFlag.class)
//                                  .filter(FeatureFlagKeys.name, FeatureName.INTEGRATION_TEST.name())
//                                  .get();
//    if (featureFlag != null) {
//      wingsPersistence.delete(FeatureFlag.class, featureFlag.getUuid());
//    }
//  }
//
//  private void shouldEnableWhenNullAccountIds() {
//    ensureDelete();
//    FeatureFlag featureFlag = FeatureFlag.builder().name(FeatureName.INTEGRATION_TEST.name()).build();
//    wingsPersistence.save(featureFlag);
//
//    featureFlagService.enableAccount(FeatureName.INTEGRATION_TEST, WingsTestConstants.ACCOUNT_ID);
//
//    assertThat(featureFlagService.isEnabled(FeatureName.INTEGRATION_TEST, WingsTestConstants.ACCOUNT_ID)).isTrue();
//  }
//
//  private void shouldEnableWhenSomeAccountsPresent() {
//    ensureDelete();
//    FeatureFlag featureFlag = FeatureFlag.builder()
//                                  .name(FeatureName.INTEGRATION_TEST.name())
//                                  .accountIds(Collections.singleton("ACCOUNT_ID_1"))
//                                  .build();
//    wingsPersistence.save(featureFlag);
//
//    featureFlagService.enableAccount(FeatureName.INTEGRATION_TEST, WingsTestConstants.ACCOUNT_ID);
//
//    assertThat(featureFlagService.isEnabled(FeatureName.INTEGRATION_TEST, WingsTestConstants.ACCOUNT_ID)).isTrue();
//  }
//}
