package software.wings.service.impl;

import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.FeatureName.ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS;
import static software.wings.beans.FeatureName.CV_DEMO;
import static software.wings.beans.FeatureName.GLOBAL_DISABLE_HEALTH_CHECK;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;

import java.util.Optional;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeatureFlagServiceImplTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Inject FeatureFlagService featureFlagService;

  @Before
  public void setup() {}

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetFeatureFlag_WhenFlagNotPresentInDB() {
    Optional<FeatureFlag> featureFlag = featureFlagService.getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS);
    assertThat(featureFlag.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetFeatureFlag_WhenFlagPresentInDB() {
    FeatureFlag featureFlag =
        FeatureFlag.builder().name(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS.name()).enabled(true).build();
    wingsPersistence.save(featureFlag);
    Optional<FeatureFlag> featureFlagInDb = featureFlagService.getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS);
    assertThat(featureFlagInDb.isPresent()).isTrue();
    assertThat(featureFlagInDb.get().getName()).isEqualTo(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS.name());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testIsEnabled_WhenFlagNotPresentInDB() {
    boolean featureFlagEnabled = featureFlagService.isEnabled(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS, null);
    assertThat(featureFlagEnabled).isFalse();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testIsEnabled_WhenFlagPresentInDB() {
    FeatureFlag featureFlag =
        FeatureFlag.builder().name(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS.name()).enabled(true).build();
    wingsPersistence.save(featureFlag);
    boolean featureFlagEnabled = featureFlagService.isEnabled(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS, null);
    assertThat(featureFlagEnabled).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetAccountIds_ScopeAccount() {
    FeatureName featureName = CV_DEMO;
    Set<String> accountIds = featureFlagService.getAccountIds(featureName);
    assertThat(accountIds).hasSize(0);

    FeatureFlag featureFlag =
        FeatureFlag.builder().name(featureName.name()).enabled(false).accountIds(Sets.newHashSet()).build();
    wingsPersistence.save(featureFlag);
    featureFlagService.isEnabledReloadCache(featureName, "accountId");
    accountIds = featureFlagService.getAccountIds(featureName);
    assertThat(accountIds).hasSize(0);

    featureFlag.setAccountIds(Sets.newHashSet("accountId"));
    wingsPersistence.save(featureFlag);
    featureFlagService.isEnabledReloadCache(featureName, "accountId");
    accountIds = featureFlagService.getAccountIds(featureName);
    assertThat(accountIds).hasSize(1);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetAccountIds_ScopeGlobal() {
    FeatureName featureName = GLOBAL_DISABLE_HEALTH_CHECK;
    Set<String> accountIds = featureFlagService.getAccountIds(featureName);
    assertThat(accountIds).hasSize(0);

    FeatureFlag featureFlag =
        FeatureFlag.builder().name(featureName.name()).enabled(false).accountIds(Sets.newHashSet()).build();
    wingsPersistence.save(featureFlag);
    featureFlagService.isEnabledReloadCache(featureName, "accountId");
    accountIds = featureFlagService.getAccountIds(featureName);
    assertThat(accountIds).hasSize(0);

    featureFlag.setAccountIds(Sets.newHashSet("accountId"));
    wingsPersistence.save(featureFlag);
    featureFlagService.isEnabledReloadCache(featureName, "accountId");
    accountIds = featureFlagService.getAccountIds(featureName);
    assertThat(accountIds).hasSize(1);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEnableGlobally_UpdateFeatureFlag() {
    FeatureName featureName = CV_DEMO;

    FeatureFlag featureFlag =
        FeatureFlag.builder().name(featureName.name()).enabled(false).accountIds(Sets.newHashSet()).build();
    wingsPersistence.save(featureFlag);
    featureFlagService.enableGlobally(featureName);
    Optional<FeatureFlag> featureFlagOptional = featureFlagService.getFeatureFlag(featureName);
    assertThat(featureFlagOptional.isPresent()).isTrue();
    assertThat(featureFlagOptional.get().isEnabled()).isTrue();
    assertThat(featureFlagOptional.get().getName()).isEqualTo(featureFlag.getName());
  }
}