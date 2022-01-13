/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.HANTANG;
import static io.harness.rule.OwnerRule.ROHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.graphql.CloudBillingAggregate;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.billing.QLCCMAggregateOperation;

import com.healthmarketscience.sqlbuilder.SqlObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class CloudBillingAggregateTest extends CategoryTest {
  private CloudBillingAggregate cloudBillingAggregate;
  private CloudBillingAggregate rawTableCloudBillingAggregate;

  @Before
  public void setUp() {
    cloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.SUM).columnName("cost").build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testToFunctionCall() {
    SqlObject functionCall = cloudBillingAggregate.toFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("SUM(t0.cost) AS sum_cost");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testToRawTableFunctionCall() {
    rawTableCloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.SUM).columnName("cost").build();
    SqlObject functionCall = rawTableCloudBillingAggregate.toRawTableFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("SUM(t0.cost) AS sum_cost");

    rawTableCloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.AVG).columnName("cost").build();
    functionCall = rawTableCloudBillingAggregate.toRawTableFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("AVG(t0.cost) AS avg_cost");

    rawTableCloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.MIN).columnName("startTime").build();
    functionCall = rawTableCloudBillingAggregate.toRawTableFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("MIN(t0.usage_start_time) AS min_startTime");

    rawTableCloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.MAX).columnName("startTime").build();
    functionCall = rawTableCloudBillingAggregate.toRawTableFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("MAX(t0.usage_start_time) AS max_startTime");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testToAwsRawTableFunctionCall() {
    rawTableCloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.SUM).columnName("unblendedCost").build();
    SqlObject functionCall = rawTableCloudBillingAggregate.toAwsRawTableFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("SUM(t0.unblendedcost) AS sum_unblendedCost");

    rawTableCloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.AVG).columnName("blendedCost").build();
    functionCall = rawTableCloudBillingAggregate.toAwsRawTableFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("AVG(t0.blendedcost) AS avg_blendedCost");

    rawTableCloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.MIN).columnName("startTime").build();
    functionCall = rawTableCloudBillingAggregate.toAwsRawTableFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("MIN(t0.usagestartdate) AS min_startTime");

    rawTableCloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.MAX).columnName("startTime").build();
    functionCall = rawTableCloudBillingAggregate.toAwsRawTableFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("MAX(t0.usagestartdate) AS max_startTime");
  }
}
