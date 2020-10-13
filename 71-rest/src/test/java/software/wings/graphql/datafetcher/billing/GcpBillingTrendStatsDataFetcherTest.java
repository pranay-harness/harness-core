package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.GcpBillingService;
import io.harness.ccm.billing.graphql.CloudBillingAggregate;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.billing.graphql.CloudBillingTimeFilter;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingTrendStats;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

public class GcpBillingTrendStatsDataFetcherTest extends AbstractDataFetcherTestBase {
  @Mock private GcpBillingService gcpBillingService;
  @InjectMocks private GcpBillingTrendStatsDataFetcher trendStatsDataFetcher;

  private String accountId = "ACCOUNT_ID";
  private CloudBillingAggregate aggregate = CloudBillingAggregate.builder().build();
  private List<CloudBillingFilter> filters = new ArrayList<>();
  private List<CloudBillingGroupBy> groupBy = new ArrayList<>();

  private static Calendar calendar1;
  private static Calendar calendar2;

  @Before
  public void setUp() {
    calendar1 = new GregorianCalendar(2020, Calendar.JANUARY, 1);
    calendar2 = new GregorianCalendar(2020, Calendar.JANUARY, 31);

    CloudBillingFilter startTimeFilter = new CloudBillingFilter();
    startTimeFilter.setStartTime(
        CloudBillingTimeFilter.builder().value(calendar1.getTime().getTime()).operator(QLTimeOperator.AFTER).build());
    filters.add(startTimeFilter);

    CloudBillingFilter endTimeFilter = new CloudBillingFilter();
    endTimeFilter.setEndTime(
        CloudBillingTimeFilter.builder().value(calendar2.getTime().getTime()).operator(QLTimeOperator.BEFORE).build());
    filters.add(endTimeFilter);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testFetch() {
    QLData result = trendStatsDataFetcher.fetch(accountId, aggregate, filters, groupBy, Collections.emptyList());
    assertThat(result instanceof QLBillingTrendStats).isTrue();
  }
}
