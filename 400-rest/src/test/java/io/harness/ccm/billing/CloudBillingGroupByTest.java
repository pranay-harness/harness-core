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
import io.harness.ccm.billing.bigquery.TruncExpression;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.billing.graphql.CloudEntityGroupBy;
import io.harness.ccm.billing.graphql.TimeTruncGroupby;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class CloudBillingGroupByTest extends CategoryTest {
  private CloudBillingGroupBy cloudBillingGroupBy;
  private CloudBillingGroupBy cloudBillingEntityGroupBy;

  @Before
  public void setUp() {
    cloudBillingGroupBy = new CloudBillingGroupBy();
    cloudBillingEntityGroupBy = new CloudBillingGroupBy();
    cloudBillingGroupBy.setTimeTruncGroupby(
        TimeTruncGroupby.builder().resolution(TruncExpression.DatePart.DAY).build());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testToGroupbyObject() {
    Object groupbyObject = cloudBillingGroupBy.toGroupbyObject();
    assertThat(groupbyObject.toString()).isEqualTo("TIMESTAMP_TRUNC(t0.startTime,DAY) AS start_time_trunc");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testRawTableTimeToGroupbyObject() {
    Object groupbyObject = cloudBillingGroupBy.toRawTableGroupbyObject();
    assertThat(groupbyObject.toString()).isEqualTo("TIMESTAMP_TRUNC(t0.usage_start_time,DAY) AS start_time_trunc");
    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.projectId);
    groupbyObject = cloudBillingEntityGroupBy.toRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.gcpProjectId);

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.product);
    groupbyObject = cloudBillingEntityGroupBy.toRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.gcpProduct);

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.billingAccountId);
    groupbyObject = cloudBillingEntityGroupBy.toRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.gcpBillingAccountId);

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.skuId);
    groupbyObject = cloudBillingEntityGroupBy.toRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.gcpSkuId);

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.sku);
    groupbyObject = cloudBillingEntityGroupBy.toRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.gcpSkuDescription);

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.region);
    groupbyObject = cloudBillingEntityGroupBy.toRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.region);

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.labelsKey);
    groupbyObject = cloudBillingEntityGroupBy.toRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.labelsKey);

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.labelsValue);
    groupbyObject = cloudBillingEntityGroupBy.toRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.labelsValue);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testAwsRawTableTimeToGroupbyObject() {
    Object groupbyObject = cloudBillingGroupBy.toAwsRawTableGroupbyObject();
    assertThat(groupbyObject.toString()).isEqualTo("TIMESTAMP_TRUNC(t0.usagestartdate,DAY) AS start_time_trunc");

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.awsLinkedAccount);
    groupbyObject = cloudBillingEntityGroupBy.toAwsRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.awsUsageAccountId);

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.awsUsageType);
    groupbyObject = cloudBillingEntityGroupBy.toAwsRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.awsUsageType);

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.awsInstanceType);
    groupbyObject = cloudBillingEntityGroupBy.toAwsRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.awsInstanceType);

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.awsService);
    groupbyObject = cloudBillingEntityGroupBy.toAwsRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.awsServiceCode);

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.region);
    groupbyObject = cloudBillingEntityGroupBy.toAwsRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.awsRegion);

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.tagsKey);
    groupbyObject = cloudBillingEntityGroupBy.toAwsRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.tagsKey);

    cloudBillingEntityGroupBy.setEntityGroupBy(CloudEntityGroupBy.tagsValue);
    groupbyObject = cloudBillingEntityGroupBy.toAwsRawTableGroupbyObject();
    assertThat(groupbyObject).isEqualTo(RawBillingTableSchema.tagsValue);
  }
}
