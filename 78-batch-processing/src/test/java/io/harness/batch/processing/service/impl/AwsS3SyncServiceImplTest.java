package io.harness.batch.processing.service.impl;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.BatchProcessingException;
import io.harness.batch.processing.ccm.S3SyncRecord;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.security.authentication.AwsS3SyncConfig;

@RunWith(MockitoJUnitRunner.class)
public class AwsS3SyncServiceImplTest extends CategoryTest {
  @InjectMocks @Spy AwsS3SyncServiceImpl awsS3SyncService;
  @Mock BatchMainConfig configuration;

  private S3SyncRecord s3SyncRecord;

  private static final String AWS_ACCESS_KEY = "awsAccessKey";
  private static final String AWS_SECRET_KEY = "awsSecretKey";
  private static final String AWS_REGION = "awsRegion";
  private final String TEST_ACCOUNT_ID = "S3_SYNC_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_SETTING_ID = "S3_SYNC_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String BILLING_ACCOUNT_ID = "S3_SYNC_BILLING_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String BILLING_BUCKET_PATH = "S3_SYNC_BILLING_BUCKET_PATH_" + this.getClass().getSimpleName();
  private final String BILLING_BUCKET_REGION = "S3_SYNC_BILLING_BUCKET_REGION_ID_" + this.getClass().getSimpleName();
  private final String EXTERNAL_ID = "EXTERNAL_ID" + this.getClass().getSimpleName();
  private final String ROLE_ARN = "ROLE_ARN" + this.getClass().getSimpleName();

  @Before
  public void setup() {
    AwsS3SyncConfig s3SyncConfig =
        AwsS3SyncConfig.builder().awsAccessKey(AWS_ACCESS_KEY).awsSecretKey(AWS_SECRET_KEY).region(AWS_REGION).build();

    when(configuration.getAwsS3SyncConfig()).thenReturn(s3SyncConfig);

    s3SyncRecord = S3SyncRecord.builder()
                       .settingId(TEST_SETTING_ID)
                       .accountId(TEST_ACCOUNT_ID)
                       .billingBucketRegion(BILLING_BUCKET_REGION)
                       .billingBucketPath(BILLING_BUCKET_PATH)
                       .billingAccountId(BILLING_ACCOUNT_ID)
                       .roleArn(ROLE_ARN)
                       .externalId(EXTERNAL_ID)
                       .build();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testAwsS3SyncServiceImpl() {
    assertThatExceptionOfType(BatchProcessingException.class)
        .isThrownBy(() -> awsS3SyncService.syncBuckets(s3SyncRecord));
  }
}