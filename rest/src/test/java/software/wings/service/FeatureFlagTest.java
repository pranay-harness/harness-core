package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureFlag.FeatureName;
import software.wings.beans.SearchFilter;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.FeatureFlagServiceImpl;

import java.util.HashSet;
import java.util.Set;

public class FeatureFlagTest extends WingsBaseTest {
  private static final FeatureName FEATURE =
      FeatureName.ECS_CREATE_CLUSTER; // Just pick a different one if this one should be deleted.

  private static final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_ID";
  private static final String TEST_ACCOUNT_ID_X = "TEST_ACCOUNT_ID_X";
  private static final String TEST_ACCOUNT_ID_Y = "TEST_ACCOUNT_ID_Y";

  @Mock private Query<FeatureFlag> query;
  @Mock private FieldEnd end;
  @Mock private WingsPersistence wingsPersistence;

  @Inject @InjectMocks private FeatureFlagServiceImpl featureFlagService;

  private Set<String> listWith = new HashSet<>(asList(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_X));
  private Set<String> listWithout = new HashSet<>(asList(TEST_ACCOUNT_ID_X, TEST_ACCOUNT_ID_Y));

  private FeatureFlag ffTrueEmpty = FeatureFlag.builder().name(FEATURE.name()).enabled(true).build();
  private FeatureFlag ffFalseEmpty = FeatureFlag.builder().name(FEATURE.name()).enabled(false).build();
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
    when(wingsPersistence.createQuery(FeatureFlag.class)).thenReturn(query);
    when(query.field(eq("name"))).thenReturn(end);

    ffPageRequest.addFilter("name", FEATURE.name(), SearchFilter.Operator.EQ);
    ffPageRequestTypeNull.addFilter("name", null, SearchFilter.Operator.EQ);
  }

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    // we have to reset the mock after each test because of the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(wingsPersistence);
  }

  @Test
  public void testFlagTrueAccountIdMissing() {
    when(end.equal(FEATURE.name())).thenReturn(query);

    when(query.get()).thenReturn(ffTrueEmpty);
    assertThat(featureFlagService.isEnabled(FEATURE, null)).isTrue();

    when(query.get()).thenReturn(ffFalseEmpty);
    assertThat(featureFlagService.isEnabled(FEATURE, null)).isFalse();

    when(query.get()).thenReturn(ffTrueWith);
    assertThat(featureFlagService.isEnabled(FEATURE, null)).isTrue();

    when(query.get()).thenReturn(ffFalseWith);
    assertThat(featureFlagService.isEnabled(FEATURE, null)).isFalse();

    when(query.get()).thenReturn(ffTrueWithout);
    assertThat(featureFlagService.isEnabled(FEATURE, null)).isTrue();

    when(query.get()).thenReturn(ffFalseWithout);
    assertThat(featureFlagService.isEnabled(FEATURE, null)).isFalse();
  }

  @Test
  public void testFlagTrueAccountIdEmpty() {
    when(end.equal(FEATURE.name())).thenReturn(query);

    when(query.get()).thenReturn(ffTrueEmpty);
    assertThat(featureFlagService.isEnabled(FEATURE, "")).isTrue();

    when(query.get()).thenReturn(ffFalseEmpty);
    assertThat(featureFlagService.isEnabled(FEATURE, "")).isFalse();

    when(query.get()).thenReturn(ffTrueWith);
    assertThat(featureFlagService.isEnabled(FEATURE, "")).isTrue();

    when(query.get()).thenReturn(ffFalseWith);
    assertThat(featureFlagService.isEnabled(FEATURE, "")).isFalse();

    when(query.get()).thenReturn(ffTrueWithout);
    assertThat(featureFlagService.isEnabled(FEATURE, "")).isTrue();

    when(query.get()).thenReturn(ffFalseWithout);
    assertThat(featureFlagService.isEnabled(FEATURE, "")).isFalse();
  }

  @Test
  public void testNameAccountIdAndWhiteListing() {
    when(end.equal(FEATURE.name())).thenReturn(query);

    when(query.get()).thenReturn(ffTrueEmpty);
    assertThat(featureFlagService.isEnabled(FEATURE, TEST_ACCOUNT_ID)).isTrue();

    when(query.get()).thenReturn(ffFalseEmpty);
    assertThat(featureFlagService.isEnabled(FEATURE, TEST_ACCOUNT_ID)).isFalse();

    when(query.get()).thenReturn(ffTrueWith);
    assertThat(featureFlagService.isEnabled(FEATURE, TEST_ACCOUNT_ID)).isTrue();

    // ********** this tests whitelisting ****************
    when(query.get()).thenReturn(ffFalseWith);
    assertThat(featureFlagService.isEnabled(FEATURE, TEST_ACCOUNT_ID)).isTrue();
    // ***************************************************

    when(query.get()).thenReturn(ffTrueWithout);
    assertThat(featureFlagService.isEnabled(FEATURE, TEST_ACCOUNT_ID)).isTrue();

    when(query.get()).thenReturn(ffFalseWithout);
    assertThat(featureFlagService.isEnabled(FEATURE, TEST_ACCOUNT_ID)).isFalse();
  }
}
