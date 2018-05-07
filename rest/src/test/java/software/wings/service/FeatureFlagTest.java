package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.dl.HQuery.excludeAuthority;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.app.DeployMode;
import software.wings.app.MainConfiguration;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.SearchFilter;
import software.wings.dl.HQuery;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.FeatureFlagServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FeatureFlagTest extends WingsBaseTest {
  private static final FeatureName FEATURE =
      FeatureName.PIVOTAL_CLOUD_FOUNDRY_SUPPORT; // Just pick a different one if this one should be deleted.

  private static final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_ID";
  private static final String TEST_ACCOUNT_ID_X = "TEST_ACCOUNT_ID_X";
  private static final String TEST_ACCOUNT_ID_Y = "TEST_ACCOUNT_ID_Y";

  @Mock private HQuery<FeatureFlag> query;
  @Mock private FieldEnd end;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private MainConfiguration mainConfiguration;
  @Inject @InjectMocks @Spy private FeatureFlagServiceImpl featureFlagService;

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
    when(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority)).thenReturn(query);
    when(query.filter("name", FEATURE.name())).thenReturn(query);

    ffPageRequest.addFilter("name", SearchFilter.Operator.EQ, FEATURE.name());
    ffPageRequestTypeNull.addFilter("name", SearchFilter.Operator.EQ);
  }

  @Test
  public void shouldBeEnabledWhenTrue() {
    when(query.get()).thenReturn(ffTrue);
    assertThat(featureFlagService.isEnabled(FEATURE, ACCOUNT_ID)).isTrue();
  }

  @Test
  public void shouldBeDisabledWhenFalse() {
    when(query.get()).thenReturn(ffFalse);
    assertThat(featureFlagService.isEnabled(FEATURE, ACCOUNT_ID)).isFalse();
  }

  @Test
  public void shouldWorkWhenAccountIdMissing() {
    when(query.get()).thenReturn(ffTrue);
    assertThat(featureFlagService.isEnabled(FEATURE, null)).isTrue();

    when(query.get()).thenReturn(ffFalse);
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
  public void shouldBeEnabledWhenWhiteListed() {
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

  @Test
  public void testFeatureFlagEnabledInConfig() {
    Query<FeatureFlag> mockQuery = Mockito.mock(Query.class);
    when(wingsPersistence.createQuery(any(Class.class), anySet())).thenReturn(mockQuery);
    when(mockQuery.asList()).thenReturn(new ArrayList<>());
    when(mainConfiguration.getFeatureNames()).thenReturn(FEATURE.name());
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.ONPREM);

    Map<String, Boolean> featureFlags = new HashMap<>();
    doAnswer(invocation -> {
      featureFlags.put((String) invocation.getArguments()[0], (Boolean) invocation.getArguments()[1]);
      return null;
    })
        .when(featureFlagService)
        .updateFeatureFlag(anyString(), anyBoolean());

    List<FeatureFlag> expectedFeatures =
        Arrays.stream(FeatureName.values())
            .map(featureName -> {
              if (featureName.name().equals(FEATURE.name())) {
                return FeatureFlag.builder().name(featureName.name()).enabled(true).build();
              } else {
                return FeatureFlag.builder().name(featureName.name()).build();
              }
            })
            .collect(Collectors.toList());
    featureFlagService.initializeFeatureFlags();

    ArgumentCaptor<Class> argumentCaptor = ArgumentCaptor.forClass(Class.class);
    verify(wingsPersistence).createQuery(FeatureFlag.class, excludeAuthority);
    featureFlags.entrySet().stream().forEach(entry -> {
      if (entry.getKey().equals(FEATURE.name())) {
        Assertions.assertThat(entry.getValue()).isTrue();
      } else {
        Assertions.assertThat(entry.getValue()).isFalse();
      }
    });
  }

  @Test
  public void testFeatureFlagEnabledInConfigSaas() {
    Query<FeatureFlag> mockQuery = Mockito.mock(Query.class);
    when(wingsPersistence.createQuery(any(Class.class), anySet())).thenReturn(mockQuery);
    when(mockQuery.asList()).thenReturn(new ArrayList<>());
    when(mainConfiguration.getFeatureNames()).thenReturn(FEATURE.name());
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.AWS);

    Map<String, Boolean> featureFlags = new HashMap<>();
    doAnswer(invocation -> {
      featureFlags.put((String) invocation.getArguments()[0], (Boolean) invocation.getArguments()[1]);
      return null;
    })
        .when(featureFlagService)
        .updateFeatureFlag(anyString(), anyBoolean());

    List<FeatureFlag> expectedFeatures =
        Arrays.stream(FeatureName.values())
            .map(featureName -> {
              if (featureName.name().equals(FEATURE.name())) {
                return FeatureFlag.builder().name(featureName.name()).enabled(true).build();
              } else {
                return FeatureFlag.builder().name(featureName.name()).build();
              }
            })
            .collect(Collectors.toList());
    featureFlagService.initializeFeatureFlags();

    verify(wingsPersistence).createQuery(FeatureFlag.class, excludeAuthority);
    featureFlags.entrySet().stream().forEach(entry -> Assertions.assertThat(entry.getValue()).isFalse());
  }

  @Test
  public void testWithBadFlagEnabledValues() {
    Query<FeatureFlag> mockQuery = Mockito.mock(Query.class);
    when(wingsPersistence.createQuery(any(Class.class), anySet())).thenReturn(mockQuery);
    when(mockQuery.asList()).thenReturn(new ArrayList<>());
    when(mainConfiguration.getFeatureNames()).thenReturn("wrongName");
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.ONPREM);

    Map<String, Boolean> featureFlags = new HashMap<>();
    doAnswer(invocation -> {
      featureFlags.put((String) invocation.getArguments()[0], (Boolean) invocation.getArguments()[1]);
      return null;
    })
        .when(featureFlagService)
        .updateFeatureFlag(anyString(), anyBoolean());

    List<FeatureFlag> expectedFeatures = Arrays.stream(FeatureName.values())
                                             .map(featureName -> FeatureFlag.builder().name(featureName.name()).build())
                                             .collect(Collectors.toList());
    featureFlagService.initializeFeatureFlags();

    verify(wingsPersistence).createQuery(FeatureFlag.class, excludeAuthority);
    featureFlags.entrySet().stream().forEach(entry -> Assertions.assertThat(entry.getValue()).isFalse());
  }
}
