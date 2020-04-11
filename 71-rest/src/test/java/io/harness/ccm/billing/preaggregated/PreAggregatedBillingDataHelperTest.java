package io.harness.ccm.billing.preaggregated;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.Timestamp;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.SqlObject;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class PreAggregatedBillingDataHelperTest extends CategoryTest {
  @InjectMocks PreAggregatedBillingDataHelper dataHelper;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private static Calendar calendar;
  List<Condition> conditions = new ArrayList<>();
  List<SqlObject> aggregates = new ArrayList<>();

  @Before
  public void setup() {
    calendar = new GregorianCalendar(2020, Calendar.JANUARY, 1);
    Condition condition1 =
        BinaryCondition.greaterThanOrEq(PreAggregatedTableSchema.startTime, Timestamp.of(calendar.getTime()));
    Condition condition2 = BinaryCondition.equalTo(PreAggregatedTableSchema.serviceCode, "serviceCode");
    conditions.add(condition1);
    conditions.add(condition2);

    FunctionCall aggregateFunction = FunctionCall.sum().addColumnParams(PreAggregatedTableSchema.blendedCost);
    aggregates.add(aggregateFunction);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getQuery() {
    List<Object> groupBy = Arrays.asList(PreAggregatedTableSchema.usageAccountId, PreAggregatedTableSchema.serviceCode,
        PreAggregatedTableSchema.usageType, PreAggregatedTableSchema.instanceType, PreAggregatedTableSchema.region);
    String query = dataHelper.getQuery(aggregates, groupBy, conditions);
    assertThat(query).isNotNull();
  }
}