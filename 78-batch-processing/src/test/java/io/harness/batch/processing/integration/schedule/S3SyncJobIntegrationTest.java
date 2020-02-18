package io.harness.batch.processing.integration.schedule;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.batch.processing.entities.BatchJobScheduledData;
import io.harness.batch.processing.entities.BatchJobScheduledData.BatchJobScheduledDataKeys;
import io.harness.batch.processing.schedule.BatchJobRunner;
import io.harness.category.element.UnitTests;
import io.harness.ccm.BillingReportConfig;
import io.harness.ccm.CCMConfig;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.settings.SettingValue;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class S3SyncJobIntegrationTest extends CategoryTest {
  private final String TEST_ACCOUNT_ID = "S3_SYNC_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String BILLING_ACCOUNT_ID = "S3_SYNC_BILLING_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String BILLING_BUCKET_PATH = "S3_SYNC_BILLING_BUCKET_PATH_" + this.getClass().getSimpleName();
  private final String BILLING_BUCKET_REGION = "S3_SYNC_BILLING_BUCKET_REGION_ID_" + this.getClass().getSimpleName();
  private final String EXTERNAL_ID = "EXTERNAL_ID_" + this.getClass().getSimpleName();
  private final String ROLE_ARN = "ROLE_ARN_" + this.getClass().getSimpleName();

  @Autowired private HPersistence hPersistence;

  @Autowired private BatchJobRunner batchJobRunner;
  @Qualifier("s3SyncJob") @Autowired private Job s3SyncJob;

  @Before
  public void setUpData() {
    hPersistence.registerUserProvider(new ThreadLocalUserProvider());
    val batchJobDataDs = hPersistence.getDatastore(BatchJobScheduledData.class);
    batchJobDataDs.delete(batchJobDataDs.createQuery(BatchJobScheduledData.class));

    BillingReportConfig billingReportConfig = BillingReportConfig.builder()
                                                  .billingAccountId(BILLING_ACCOUNT_ID)
                                                  .billingBucketPath(BILLING_BUCKET_PATH)
                                                  .billingBucketRegion(BILLING_BUCKET_REGION)
                                                  .isBillingReportEnabled(true)
                                                  .externalId(EXTERNAL_ID)
                                                  .roleArn(ROLE_ARN)
                                                  .build();
    CCMConfig ccmConfig = CCMConfig.builder().cloudCostEnabled(true).billingReportConfig(billingReportConfig).build();

    SettingValue settingValue = AwsConfig.builder().ccmConfig(ccmConfig).build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(TEST_ACCOUNT_ID)
                                            .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                                            .withValue(settingValue)
                                            .build();
    hPersistence.save(settingAttribute);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldRunS3SyncJob() throws Exception {
    val batchJobScheduledDataDs = hPersistence.getDatastore(BatchJobScheduledData.class);
    long count = batchJobScheduledDataDs.getCount(batchJobScheduledDataDs.createQuery(BatchJobScheduledData.class)
                                                      .filter(BatchJobScheduledDataKeys.accountId, TEST_ACCOUNT_ID));
    batchJobRunner.runJob(TEST_ACCOUNT_ID, s3SyncJob);
    assertThat(count).isEqualTo(0);
  }

  @After
  public void clearCollection() {
    val batchJobDataDs = hPersistence.getDatastore(BatchJobScheduledData.class);
    batchJobDataDs.delete(batchJobDataDs.createQuery(BatchJobScheduledData.class));

    val settingAttributesDs = hPersistence.getDatastore(SettingAttribute.class);
    settingAttributesDs.delete(settingAttributesDs.createQuery(SettingAttribute.class)
                                   .filter(SettingAttributeKeys.accountId, TEST_ACCOUNT_ID));

    val batchJobScheduledDataDs = hPersistence.getDatastore(BatchJobScheduledData.class);
    batchJobScheduledDataDs.delete(batchJobScheduledDataDs.createQuery(BatchJobScheduledData.class)
                                       .filter(BatchJobScheduledDataKeys.accountId, TEST_ACCOUNT_ID));
  }
}
