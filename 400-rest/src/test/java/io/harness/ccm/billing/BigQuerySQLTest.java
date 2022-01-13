/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.HANTANG;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.bigquery.BigQuerySQL;
import io.harness.ccm.billing.graphql.CloudEntityGroupBy;
import io.harness.rule.Owner;

import com.google.cloud.Timestamp;
import com.healthmarketscience.sqlbuilder.BetweenCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgLimitClause;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class BigQuerySQLTest extends CategoryTest {
  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testBuild() {
    Calendar calendar1 = new GregorianCalendar(2019, Calendar.DECEMBER, 1);
    Calendar calendar2 = new GregorianCalendar(2019, Calendar.DECEMBER, 31);

    List<Condition> conditions = new ArrayList<>();
    Condition condition = new BetweenCondition(
        RawBillingTableSchema.startTime, Timestamp.of(calendar1.getTime()), Timestamp.of(calendar2.getTime()));
    conditions.add(condition);

    Object groupbyObject = CloudEntityGroupBy.product;

    FunctionCall aggregateFunction = FunctionCall.sum().addColumnParams(RawBillingTableSchema.cost);
    BigQuerySQL bigQuerySQL =
        BigQuerySQL.builder().groupbyObjects(Arrays.asList(groupbyObject)).conditions(conditions).build();
    String query = bigQuerySQL.getQuery().validate().toString();
    log.info(query);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldBuildWithoutGroupby() {
    Calendar calendar1 = new GregorianCalendar(2019, Calendar.DECEMBER, 1);
    Calendar calendar2 = new GregorianCalendar(2019, Calendar.DECEMBER, 31);

    List<Condition> conditions = new ArrayList<>();
    Condition condition = new BetweenCondition(
        RawBillingTableSchema.startTime, Timestamp.of(calendar1.getTime()), Timestamp.of(calendar2.getTime()));
    conditions.add(condition);

    Object groupbyObject = null;

    FunctionCall aggregateFunction = FunctionCall.sum().addColumnParams(RawBillingTableSchema.cost);
    String query = BigQuerySQL.builder()
                       .groupbyObjects(Arrays.asList(groupbyObject))
                       .conditions(conditions)
                       .pgLimit(new PgLimitClause(100))
                       .build()
                       .getQuery()
                       .validate()
                       .toString();
    log.info(query);
  }
}
