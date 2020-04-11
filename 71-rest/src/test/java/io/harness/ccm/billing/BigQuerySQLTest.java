package io.harness.ccm.billing;

import static io.harness.rule.OwnerRule.HANTANG;

import com.google.cloud.Timestamp;

import com.healthmarketscience.sqlbuilder.BetweenCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgLimitClause;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.bigquery.BigQuerySQL;
import io.harness.ccm.billing.graphql.OutOfClusterEntityGroupBy;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

@Slf4j
public class BigQuerySQLTest {
  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testBuild() {
    Calendar calendar1 = new GregorianCalendar(2019, Calendar.DECEMBER, 1);
    Calendar calendar2 = new GregorianCalendar(2019, Calendar.DECEMBER, 31);

    List<Condition> conditions = new ArrayList<>();
    Condition condition = new BetweenCondition(
        GcpBillingTableSchema.usageStartTime, Timestamp.of(calendar1.getTime()), Timestamp.of(calendar2.getTime()));
    conditions.add(condition);

    Object groupbyObject = OutOfClusterEntityGroupBy.product;

    FunctionCall aggregateFunction = FunctionCall.sum().addColumnParams(GcpBillingTableSchema.cost);
    BigQuerySQL bigQuerySQL =
        BigQuerySQL.builder().groupbyObjects(Arrays.asList(groupbyObject)).conditions(conditions).build();
    String query = bigQuerySQL.getQuery().validate().toString();
    logger.info(query);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldBuildWithoutGroupby() {
    Calendar calendar1 = new GregorianCalendar(2019, Calendar.DECEMBER, 1);
    Calendar calendar2 = new GregorianCalendar(2019, Calendar.DECEMBER, 31);

    List<Condition> conditions = new ArrayList<>();
    Condition condition = new BetweenCondition(
        GcpBillingTableSchema.usageStartTime, Timestamp.of(calendar1.getTime()), Timestamp.of(calendar2.getTime()));
    conditions.add(condition);

    Object groupbyObject = null;

    FunctionCall aggregateFunction = FunctionCall.sum().addColumnParams(GcpBillingTableSchema.cost);
    String query = BigQuerySQL.builder()
                       .groupbyObjects(Arrays.asList(groupbyObject))
                       .conditions(conditions)
                       .pgLimit(new PgLimitClause(100))
                       .build()
                       .getQuery()
                       .validate()
                       .toString();
    logger.info(query);
  }
}
