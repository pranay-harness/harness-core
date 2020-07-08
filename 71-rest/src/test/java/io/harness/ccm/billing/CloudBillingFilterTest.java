package io.harness.ccm.billing;

import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_GCP_STARTTIME;
import static io.harness.rule.OwnerRule.HANTANG;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.healthmarketscience.sqlbuilder.Condition;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingIdFilter;
import io.harness.ccm.billing.graphql.CloudBillingTimeFilter;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;

public class CloudBillingFilterTest extends CategoryTest {
  private Number value = 0;
  private CloudBillingFilter cloudBillingFilter;
  private CloudBillingFilter rawTableCloudBillingFilter;

  @Before
  public void setUp() {
    cloudBillingFilter = new CloudBillingFilter();
    rawTableCloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setStartTime(CloudBillingTimeFilter.builder()
                                        .operator(QLTimeOperator.AFTER)
                                        .variable(BILLING_GCP_STARTTIME)
                                        .value(value)
                                        .build());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testToCondition() {
    Condition condition = cloudBillingFilter.toCondition();
    assertThat(condition.toString()).isEqualTo("(t0.startTime >= '1970-01-01T00:00:00Z')");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testRawTableToConditionStartTime() {
    rawTableCloudBillingFilter.setStartTime(
        CloudBillingTimeFilter.builder().operator(QLTimeOperator.AFTER).value(value).build());
    Condition condition = rawTableCloudBillingFilter.toRawTableCondition();
    assertThat(condition.toString()).isEqualTo("(t0.usage_start_time >= '1970-01-01T00:00:00Z')");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testRawTableToConditionEndTime() {
    rawTableCloudBillingFilter.setEndTime(
        CloudBillingTimeFilter.builder().operator(QLTimeOperator.AFTER).value(value).build());
    Condition condition = rawTableCloudBillingFilter.toRawTableCondition();
    assertThat(condition.toString()).isEqualTo("(t0.usage_end_time >= '1970-01-01T00:00:00Z')");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testRawTableToConditionPreAggStartTime() {
    rawTableCloudBillingFilter.setPreAggregatedTableStartTime(
        CloudBillingTimeFilter.builder().operator(QLTimeOperator.AFTER).value(value).build());
    Condition condition = rawTableCloudBillingFilter.toRawTableCondition();
    assertThat(condition.toString()).isEqualTo("(t0.usage_start_time >= '1970-01-01T00:00:00Z')");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testRawTableToConditionPreAggEndTime() {
    rawTableCloudBillingFilter.setPreAggregatedTableEndTime(
        CloudBillingTimeFilter.builder().operator(QLTimeOperator.AFTER).value(value).build());
    Condition condition = rawTableCloudBillingFilter.toRawTableCondition();
    assertThat(condition.toString()).isEqualTo("(t0.usage_start_time >= '1970-01-01T00:00:00Z')");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testRawTableToConditionProjectId() {
    String[] ProjectId = {"ccm-play"};
    rawTableCloudBillingFilter.setProjectId(
        CloudBillingIdFilter.builder().operator(QLIdOperator.EQUALS).values(ProjectId).build());
    Condition condition = rawTableCloudBillingFilter.toRawTableCondition();
    assertThat(condition.toString()).isEqualTo("(t0.project.id = 'ccm-play')");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testRawTableToConditionProduct() {
    String[] product = {"bigQuery", "stackDriver"};
    rawTableCloudBillingFilter.setProduct(
        CloudBillingIdFilter.builder().operator(QLIdOperator.IN).values(product).build());
    Condition condition = rawTableCloudBillingFilter.toRawTableCondition();
    assertThat(condition.toString()).isEqualTo("(t0.service.description IN ('bigQuery','stackDriver') )");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testRawTableToConditionSku() {
    String[] sku = {"sku"};
    rawTableCloudBillingFilter.setSku(CloudBillingIdFilter.builder().operator(QLIdOperator.EQUALS).values(sku).build());
    Condition condition = rawTableCloudBillingFilter.toRawTableCondition();
    assertThat(condition.toString()).isEqualTo("(t0.sku.description = 'sku')");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testRawTableToConditionBillingAccountId() {
    String[] account = {"account1"};
    rawTableCloudBillingFilter.setBillingAccountId(
        CloudBillingIdFilter.builder().operator(QLIdOperator.EQUALS).values(account).build());
    Condition condition = rawTableCloudBillingFilter.toRawTableCondition();
    assertThat(condition.toString()).isEqualTo("(t0.billing_account_id = 'account1')");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testRawTableToConditionRegion() {
    String[] ProjectId = {"us-east-1", "us-east-2"};
    rawTableCloudBillingFilter.setRegion(
        CloudBillingIdFilter.builder().operator(QLIdOperator.IN).values(ProjectId).build());
    Condition condition = rawTableCloudBillingFilter.toRawTableCondition();
    assertThat(condition.toString()).isEqualTo("(t0.location.region IN ('us-east-1','us-east-2') )");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testRawTableToConditionLabelsKey() {
    String[] key = {"key1"};
    rawTableCloudBillingFilter.setLabelsKey(
        CloudBillingIdFilter.builder().operator(QLIdOperator.EQUALS).values(key).build());
    Condition condition = rawTableCloudBillingFilter.toRawTableCondition();
    assertThat(condition.toString()).isEqualTo("(labels.key = 'key1')");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testRawTableToConditionLabelsValue() {
    String[] value = {"val1"};
    rawTableCloudBillingFilter.setLabelsValue(
        CloudBillingIdFilter.builder().operator(QLIdOperator.EQUALS).values(value).build());
    Condition condition = rawTableCloudBillingFilter.toRawTableCondition();
    assertThat(condition.toString()).isEqualTo("(labels.value = 'val1')");
  }
}
