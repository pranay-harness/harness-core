package software.wings.resources.stats.model;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey;
import software.wings.beans.infrastructure.instance.stats.ServerlessInstanceStats;
import software.wings.service.impl.instance.ServerlessTestHelper;

import java.util.Arrays;
import java.util.Collections;

public class ServerlessInstanceTimelineTest extends CategoryTest {
  public static final String ACCOUNTID = "accountid";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_create() {
    final ServerlessInstanceTimeline serverlessInstanceTimeline = ServerlessInstanceTimeline.create(
        Arrays.asList(getServerlessInstanceStats()), Collections.emptySet(), InvocationCountKey.LAST_30_DAYS);
    assertThat(serverlessInstanceTimeline.getPoints().get(0).getTotalInvocationCount()).isEqualTo(110);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_copyWithLimit() {
    final ServerlessInstanceTimeline serverlessInstanceTimeline = ServerlessInstanceTimeline.create(
        Arrays.asList(getServerlessInstanceStats()), Collections.emptySet(), InvocationCountKey.LAST_30_DAYS);
    assertThat(serverlessInstanceTimeline.getPoints().get(0).getTotalInvocationCount()).isEqualTo(110);

    final ServerlessInstanceTimeline serverlessInstanceTimeline1 =
        ServerlessInstanceTimeline.copyWithLimit(serverlessInstanceTimeline, 1);
    assertThat(serverlessInstanceTimeline1.getPoints().get(0).getAggregateInvocationCountList().size()).isEqualTo(1);
  }

  private ServerlessInstanceStats getServerlessInstanceStats() {
    return ServerlessTestHelper.getServerlessInstanceStats();
  }
}