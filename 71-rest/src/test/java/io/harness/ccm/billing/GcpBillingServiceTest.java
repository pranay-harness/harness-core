package io.harness.ccm.billing;

import static com.google.cloud.bigquery.StandardSQLTypeName.FLOAT64;
import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.Timestamp;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.EmptyTableResult;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

public class GcpBillingServiceTest extends CategoryTest {
  @Mock BigQuery bigQuery;
  @Mock TableResult tableResult;
  @Mock BigQueryService bigQueryService;
  @InjectMocks GcpBillingService gcpBillingService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() throws InterruptedException {
    tableResult = new EmptyTableResult(Schema.of(Field.newBuilder("cost", (StandardSQLTypeName) FLOAT64).build()));
    when(bigQueryService.get()).thenReturn(bigQuery);
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void ShouldGetGcpBillingTimeSeriesStats() throws InterruptedException {
    Calendar calendar1 = new GregorianCalendar(2020, Calendar.JANUARY, 1);
    Calendar calendar2 = new GregorianCalendar(2020, Calendar.JANUARY, 31);

    List<Condition> conditions = new ArrayList<>();
    Condition condition1 =
        BinaryCondition.greaterThanOrEq(GcpBillingTableSchema.usageStartTime, Timestamp.of(calendar1.getTime()));
    Condition condition2 =
        BinaryCondition.greaterThanOrEq(GcpBillingTableSchema.usageEndTime, Timestamp.of(calendar2.getTime()));
    conditions.add(condition1);
    conditions.add(condition2);

    List<Object> gcpBillingGroupby = Collections.EMPTY_LIST;
    FunctionCall aggregateFunction = FunctionCall.sum().addColumnParams(GcpBillingTableSchema.cost);

    GcpBillingTimeSeriesStatsDTO timeSeriesStats =
        gcpBillingService.getGcpBillingTimeSeriesStats(aggregateFunction, gcpBillingGroupby, conditions);
    assertThat(timeSeriesStats).isNull();
  }
}