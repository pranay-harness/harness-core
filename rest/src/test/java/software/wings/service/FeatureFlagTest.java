package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.hazelcast.logging.LogEvent;
import org.apache.log4j.Appender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureFlag.FeatureName;
import software.wings.beans.SearchFilter;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

// TODO - add this to test logged error messages
//@RunWith(MockitoJUnitRunner.class)
public class FeatureFlagTest extends WingsBaseTest {
  @Mock private Appender mockAppender;
  @Captor private ArgumentCaptor<LogEvent> captorLoggingEvent;

  private Logger logger;

  /**
   * The Query.
   */
  @Mock Query<FeatureFlag> query;
  /**
   * The End.
   */
  @Mock FieldEnd end;
  @Mock private WingsPersistence wingsPersistence;

  @Inject @InjectMocks private FeatureFlagService featureFlagService;

  private final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_ID";
  private final String TEST_ACCOUNT_ID_X = "TEST_ACCOUNT_ID_X";
  private final String TEST_ACCOUNT_ID_Y = "TEST_ACCOUNT_ID_Y";
  private Set<String> listWith = new HashSet<>(Arrays.asList(TEST_ACCOUNT_ID, TEST_ACCOUNT_ID_X));
  private Set<String> listWithout = new HashSet<>(Arrays.asList(TEST_ACCOUNT_ID_X, TEST_ACCOUNT_ID_Y));

  private FeatureFlag ffTrueEmpty = FeatureFlag.builder().name(FeatureName.GIT_SYNC).enabled(true).build();
  private FeatureFlag ffFalseEmpty = FeatureFlag.builder().name(FeatureName.GIT_SYNC).enabled(false).build();
  private FeatureFlag ffTrueWith =
      FeatureFlag.builder().name(FeatureName.GIT_SYNC).enabled(true).whiteListedAccountIds(listWith).build();
  private FeatureFlag ffFalseWith =
      FeatureFlag.builder().name(FeatureName.GIT_SYNC).enabled(false).whiteListedAccountIds(listWith).build();
  private FeatureFlag ffTrueWithout =
      FeatureFlag.builder().name(FeatureName.GIT_SYNC).enabled(true).whiteListedAccountIds(listWithout).build();
  private FeatureFlag ffFalseWithout =
      FeatureFlag.builder().name(FeatureName.GIT_SYNC).enabled(false).whiteListedAccountIds(listWithout).build();

  private PageRequest<FeatureFlag> ffPageRequest = new PageRequest<>();
  private PageRequest<FeatureFlag> ffPageRequestTypeNull = new PageRequest<>();

  /**
   * setup for test.
   */
  @Before
  public void setUp() throws Exception {
    /* TODO - add this to test logged error messages
    // prepare the appender so Log4j likes it
    when(mockAppender.getName()).thenReturn("MockAppender");
    when(mockAppender.isStarted()).thenReturn(true);
    when(mockAppender.isStopped()).thenReturn(false);

    logger = LoggerFactory.getLogger(getClass());
    logger.addAppender(mockAppender);
    logger.setLevel(Level.INFO);
    */

    when(wingsPersistence.createQuery(FeatureFlag.class)).thenReturn(query);
    when(query.field(eq("name"))).thenReturn(end);

    ffPageRequest.addFilter("name", FeatureName.GIT_SYNC, SearchFilter.Operator.EQ);
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
  public void testTypeNull() {
    when(end.equal(null)).thenReturn(query);

    when(query.get()).thenReturn(ffTrueEmpty);
    assertThat(featureFlagService.isEnabled(null, TEST_ACCOUNT_ID)).isFalse();

    when(query.get()).thenReturn(ffFalseEmpty);
    assertThat(featureFlagService.isEnabled(null, TEST_ACCOUNT_ID)).isFalse();

    when(query.get()).thenReturn(ffTrueWith);
    assertThat(featureFlagService.isEnabled(null, TEST_ACCOUNT_ID)).isFalse();

    when(query.get()).thenReturn(ffFalseWith);
    assertThat(featureFlagService.isEnabled(null, TEST_ACCOUNT_ID)).isFalse();

    when(query.get()).thenReturn(ffTrueWithout);
    assertThat(featureFlagService.isEnabled(null, TEST_ACCOUNT_ID)).isFalse();

    when(query.get()).thenReturn(ffFalseWithout);
    assertThat(featureFlagService.isEnabled(null, TEST_ACCOUNT_ID)).isFalse();
  }

  @Test
  public void testFlagTrueAccountIdMissing() {
    when(end.equal(FeatureName.GIT_SYNC)).thenReturn(query);

    when(query.get()).thenReturn(ffTrueEmpty);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, null)).isTrue();
    // TODO - add this to test logged error messages
    // verifyErrorMessages("FeatureFlag accountId is null or missing!");

    when(query.get()).thenReturn(ffFalseEmpty);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, null)).isFalse();

    when(query.get()).thenReturn(ffTrueWith);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, null)).isTrue();

    when(query.get()).thenReturn(ffFalseWith);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, null)).isFalse();

    when(query.get()).thenReturn(ffTrueWithout);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, null)).isTrue();

    when(query.get()).thenReturn(ffFalseWithout);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, null)).isFalse();
  }

  @Test
  public void testFlagTrueAccountIdEmpty() {
    when(end.equal(FeatureName.GIT_SYNC)).thenReturn(query);

    when(query.get()).thenReturn(ffTrueEmpty);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, "")).isTrue();

    when(query.get()).thenReturn(ffFalseEmpty);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, "")).isFalse();

    when(query.get()).thenReturn(ffTrueWith);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, "")).isTrue();

    when(query.get()).thenReturn(ffFalseWith);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, "")).isFalse();

    when(query.get()).thenReturn(ffTrueWithout);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, "")).isTrue();

    when(query.get()).thenReturn(ffFalseWithout);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, "")).isFalse();
  }

  @Test
  public void testTypeAccountIdAndWhiteListing() {
    when(end.equal(FeatureName.GIT_SYNC)).thenReturn(query);

    when(query.get()).thenReturn(ffTrueEmpty);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, TEST_ACCOUNT_ID)).isTrue();
    // TODO - add this to test logged error messages
    // verifyErrorMessages("FeatureFlag accountId is null or missing!");

    when(query.get()).thenReturn(ffFalseEmpty);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, TEST_ACCOUNT_ID)).isFalse();

    when(query.get()).thenReturn(ffTrueWith);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, TEST_ACCOUNT_ID)).isTrue();

    // ********** this tests whitelisting ****************
    when(query.get()).thenReturn(ffFalseWith);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, TEST_ACCOUNT_ID)).isTrue();
    // ***************************************************

    when(query.get()).thenReturn(ffTrueWithout);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, TEST_ACCOUNT_ID)).isTrue();

    when(query.get()).thenReturn(ffFalseWithout);
    assertThat(featureFlagService.isEnabled(FeatureName.GIT_SYNC, TEST_ACCOUNT_ID)).isFalse();
  }
}
