package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.app.DeployMode;
import software.wings.app.MainConfiguration;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.FeatureFlagServiceImpl;

import java.util.HashSet;
import java.util.Set;

public class FeatureFlagTest extends WingsBaseTest {
  private static final FeatureName FEATURE =
      FeatureName.CV_DEMO; // Just pick a different one if this one should be deleted.

  private static final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_ID";
  private static final String TEST_ACCOUNT_ID_X = "TEST_ACCOUNT_ID_X";
  private static final String TEST_ACCOUNT_ID_Y = "TEST_ACCOUNT_ID_Y";

  @Mock private MainConfiguration mainConfiguration;
  @Inject @InjectMocks @Spy private FeatureFlagServiceImpl featureFlagService;

  @Inject private WingsPersistence wingsPersistence;

  private Set<String> listWith = new HashSet<>(asList(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_X));
  private Set<String> listWithout = new HashSet<>(asList(TEST_ACCOUNT_ID_X, TEST_ACCOUNT_ID_Y));

  private FeatureFlag ffTrue = FeatureFlag.builder().name(FEATURE.name()).enabled(true).build();
  private FeatureFlag ffFalse = FeatureFlag.builder().name(FEATURE.name()).enabled(false).build();
  private FeatureFlag ffTrueWith =
      FeatureFlag.builder().name(FEATURE.name()).enabled(true).accountIds(listWith).build();
  private FeatureFlag ffFalseWith =
      FeatureFlag.builder().name(FEATURE.name()).enabled(false).accountIds(listWith).build();
  private FeatureFlag ffTrueWithout =
      FeatureFlag.builder().name(FEATURE.name()).enabled(true).accountIds(listWithout).build();
  private FeatureFlag ffFalseWithout =
      FeatureFlag.builder().name(FEATURE.name()).enabled(false).accountIds(listWithout).build();

  private PageRequest<FeatureFlag> ffPageRequest = new PageRequest<>();
  private PageRequest<FeatureFlag> ffPageRequestTypeNull = new PageRequest<>();

  /**
   * setup for test.
   */
  @Before
  public void setUp() throws Exception {
    ffPageRequest.addFilter("name", SearchFilter.Operator.EQ, FEATURE.name());
    ffPageRequestTypeNull.addFilter("name", SearchFilter.Operator.EQ);
  }

  @Test
  public void shouldBeEnabledWhenTrue() {
    wingsPersistence.save(ffTrue);
    assertThat(featureFlagService.isEnabled(FEATURE, ACCOUNT_ID)).isTrue();
  }

  @Test
  public void shouldBeDisabledWhenFalse() {
    wingsPersistence.save(ffFalse);
    assertThat(featureFlagService.isEnabled(FEATURE, ACCOUNT_ID)).isFalse();
  }

  @Test
  public void shouldWorkWhenAccountIdMissingTrue() {
    wingsPersistence.save(ffTrue);
    assertThat(featureFlagService.isEnabled(FEATURE, null)).isTrue();
  }

  @Test
  public void shouldWorkWhenAccountIdMissingFalse() {
    wingsPersistence.save(ffFalse);
    assertThat(featureFlagService.isEnabled(FEATURE, null)).isFalse();
  }

  @Test
  public void shouldWorkWhenAccountIdMissingTrueWith() {
    wingsPersistence.save(ffTrueWith);
    assertThat(featureFlagService.isEnabled(FEATURE, null)).isTrue();
  }

  @Test
  public void shouldWorkWhenAccountIdMissingFalseWith() {
    wingsPersistence.save(ffFalseWith);
    assertThat(featureFlagService.isEnabled(FEATURE, null)).isFalse();
  }

  @Test
  public void shouldWorkWhenAccountIdMissingTrueWithout() {
    wingsPersistence.save(ffTrueWithout);
    assertThat(featureFlagService.isEnabled(FEATURE, null)).isTrue();
  }

  @Test
  public void shouldWorkWhenAccountIdMissingFalseWithout() {
    wingsPersistence.save(ffFalseWithout);
    assertThat(featureFlagService.isEnabled(FEATURE, null)).isFalse();
  }

  @Test
  public void shouldBeEnabledWhenWhiteListedTrueWith() {
    wingsPersistence.save(ffTrueWith);
    assertThat(featureFlagService.isEnabled(FEATURE, TEST_ACCOUNT_ID)).isTrue();
  }

  @Test
  public void shouldBeEnabledWhenWhiteListedFalseWith() {
    // This tests whitelisting
    wingsPersistence.save(ffFalseWith);
    assertThat(featureFlagService.isEnabled(FEATURE, TEST_ACCOUNT_ID)).isTrue();
  }

  @Test
  public void shouldBeEnabledWhenWhiteListedTrueWithout() {
    wingsPersistence.save(ffTrueWithout);
    assertThat(featureFlagService.isEnabled(FEATURE, TEST_ACCOUNT_ID)).isTrue();
  }

  @Test
  public void shouldBeEnabledWhenWhiteListedFalseWithout() {
    wingsPersistence.save(ffFalseWithout);
    assertThat(featureFlagService.isEnabled(FEATURE, TEST_ACCOUNT_ID)).isFalse();
  }

  @Test
  public void testFeatureFlagEnabledInConfig() {
    when(mainConfiguration.getFeatureNames()).thenReturn(FEATURE.name());
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.ONPREM);

    featureFlagService.initializeFeatureFlags();

    for (FeatureName featureName : FeatureName.values()) {
      assertThat(featureFlagService.isEnabled(featureName, null)).isEqualTo(featureName == FEATURE);
    }
  }

  @Test
  public void testFeatureFlagEnabledInConfigSaas() {
    when(mainConfiguration.getFeatureNames()).thenReturn(FEATURE.name());
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES);

    featureFlagService.initializeFeatureFlags();

    for (FeatureName featureName : FeatureName.values()) {
      assertThat(featureFlagService.isEnabled(featureName, null)).isFalse();
    }
  }

  @Test
  public void testWithBadFlagEnabledValues() {
    when(mainConfiguration.getFeatureNames()).thenReturn("wrongName");
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.ONPREM);

    featureFlagService.initializeFeatureFlags();

    for (FeatureName featureName : FeatureName.values()) {
      assertThat(featureFlagService.isEnabled(featureName, null)).isFalse();
    }
  }
}
