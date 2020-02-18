package io.harness.batch.processing.writer;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import io.harness.CategoryTest;
import io.harness.batch.processing.service.impl.AwsS3SyncServiceImpl;
import io.harness.category.element.UnitTests;
import io.harness.ccm.BillingReportConfig;
import io.harness.ccm.CCMConfig;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.settings.SettingValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class S3SyncEventWriterTest extends CategoryTest {
  @InjectMocks S3SyncEventWriter s3SyncEventWriter;
  @Mock private AwsS3SyncServiceImpl awsS3SyncService;

  private final String TEST_ACCOUNT_ID = "S3_SYNC_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String BILLING_ACCOUNT_ID = "S3_SYNC_BILLING_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String BILLING_BUCKET_PATH = "S3_SYNC_BILLING_BUCKET_PATH_" + this.getClass().getSimpleName();
  private final String BILLING_BUCKET_REGION = "S3_SYNC_BILLING_BUCKET_REGION_ID_" + this.getClass().getSimpleName();
  private final String EXTERNAL_ID = "EXTERNAL_ID" + this.getClass().getSimpleName();
  private final String ROLE_ARN = "ROLE_ARN" + this.getClass().getSimpleName();

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldWritePodUtilizationMetrics() {
    Mockito.doNothing().when(awsS3SyncService).syncBuckets(any());
    BillingReportConfig billingReportConfig = BillingReportConfig.builder()
                                                  .billingAccountId(BILLING_ACCOUNT_ID)
                                                  .billingBucketPath(BILLING_BUCKET_PATH)
                                                  .billingBucketRegion(BILLING_BUCKET_REGION)
                                                  .isBillingReportEnabled(true)
                                                  .roleArn(ROLE_ARN)
                                                  .externalId(EXTERNAL_ID)
                                                  .build();
    CCMConfig ccmConfig = CCMConfig.builder().cloudCostEnabled(true).billingReportConfig(billingReportConfig).build();

    SettingValue settingValue = AwsConfig.builder().ccmConfig(ccmConfig).build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(TEST_ACCOUNT_ID)
                                            .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                                            .withValue(settingValue)
                                            .build();

    List<SettingAttribute> settingAttributeList = Arrays.asList(settingAttribute);
    s3SyncEventWriter.write(settingAttributeList);
    Collection<Invocation> invocations = Mockito.mockingDetails(awsS3SyncService).getInvocations();
    int numberOfCalls = invocations.size();
    assertThat(numberOfCalls).isEqualTo(1);
  }
}
