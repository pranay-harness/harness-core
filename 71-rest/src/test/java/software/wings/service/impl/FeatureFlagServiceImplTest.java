package software.wings.service.impl;

import static io.harness.rule.OwnerRule.MEHUL;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.rule.OwnerRule.VIKAS;

import static software.wings.beans.FeatureName.CV_DEMO;
import static software.wings.beans.FeatureName.GLOBAL_DISABLE_HEALTH_CHECK;
import static software.wings.beans.FeatureName.SEARCH_REQUEST;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.FeatureFlag;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
    Optional<FeatureFlag> featureFlag = featureFlagService.getFeatureFlag(SEARCH_REQUEST);
    assertThat(featureFlag.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetFeatureFlag_WhenFlagPresentInDB() {
    FeatureFlag featureFlag = FeatureFlag.builder().name(SEARCH_REQUEST.name()).enabled(true).build();
    wingsPersistence.save(featureFlag);
    Optional<FeatureFlag> featureFlagInDb = featureFlagService.getFeatureFlag(SEARCH_REQUEST);
    assertThat(featureFlagInDb.isPresent()).isTrue();
    assertThat(featureFlagInDb.get().getName()).isEqualTo(SEARCH_REQUEST.name());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testIsEnabled_WhenFlagNotPresentInDB() {
    boolean featureFlagEnabled = featureFlagService.isEnabled(SEARCH_REQUEST, null);
    assertThat(featureFlagEnabled).isFalse();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testIsEnabled_WhenFlagPresentInDB() {
    FeatureFlag featureFlag = FeatureFlag.builder().name(SEARCH_REQUEST.name()).enabled(true).build();
    wingsPersistence.save(featureFlag);
    boolean featureFlagEnabled = featureFlagService.isEnabled(SEARCH_REQUEST, null);
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

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateFeatureFlagForAccount_FeatureFlagNotFound() {
    featureFlagService.updateFeatureFlagForAccount(CV_DEMO.name(), "abcde", false);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateFeatureFlagForAccount_EnableFeatureFlag() {
    FeatureName featureName = CV_DEMO;
    FeatureFlag featureFlag = FeatureFlag.builder().name(featureName.name()).enabled(false).obsolete(false).build();
    wingsPersistence.save(featureFlag);
    FeatureFlag updatedFeatureFlag = featureFlagService.updateFeatureFlagForAccount(featureName.name(), "abcde", true);
    assertThat(updatedFeatureFlag).isNotNull();
    assertThat(updatedFeatureFlag.getAccountIds()).contains("abcde");
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateFeatureFlagForAccount_DisableFeatureFlag() {
    FeatureName featureName = CV_DEMO;
    FeatureFlag featureFlag = FeatureFlag.builder()
                                  .name(featureName.name())
                                  .enabled(false)
                                  .accountIds(Sets.newHashSet("abcde"))
                                  .obsolete(false)
                                  .build();
    wingsPersistence.save(featureFlag);
    FeatureFlag updatedFeatureFlag = featureFlagService.updateFeatureFlagForAccount(featureName.name(), "abcde", false);
    assertThat(updatedFeatureFlag).isNotNull();
    assertThat(updatedFeatureFlag.getAccountIds()).isEmpty();
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void testRemoveAccountReferenceFromAllFeatureFlags() {
    FeatureName featureName1 = CV_DEMO;
    FeatureName featureName2 = SEARCH_REQUEST;
    FeatureFlag featureFlag1 = FeatureFlag.builder()
                                   .name(featureName1.name())
                                   .enabled(false)
                                   .accountIds(Sets.newHashSet(ACCOUNT_ID, "abc", "def", "ghi"))
                                   .obsolete(false)
                                   .build();
    FeatureFlag featureFlag2 = FeatureFlag.builder()
                                   .name(featureName2.name())
                                   .enabled(false)
                                   .accountIds(Sets.newHashSet(ACCOUNT_ID, "jkl", "mno", "pqr"))
                                   .obsolete(false)
                                   .build();
    wingsPersistence.save(featureFlag1);
    wingsPersistence.save(featureFlag2);
    featureFlagService.removeAccountReferenceFromAllFeatureFlags(ACCOUNT_ID);
    FeatureFlag updatedFeatureFlag1 = featureFlagService.getFeatureFlag(featureName1).get();
    assertThat(updatedFeatureFlag1).isNotNull();
    assertThat(updatedFeatureFlag1.getAccountIds()).containsExactlyInAnyOrder("abc", "def", "ghi");
    FeatureFlag updatedFeatureFlag2 = featureFlagService.getFeatureFlag(featureName2).get();
    assertThat(updatedFeatureFlag2).isNotNull();
    assertThat(updatedFeatureFlag2.getAccountIds()).containsExactlyInAnyOrder("jkl", "mno", "pqr");
  }
}
