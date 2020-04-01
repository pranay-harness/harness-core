package io.harness.batch.processing.writer;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import io.harness.CategoryTest;
import io.harness.batch.processing.service.impl.AwsS3SyncServiceImpl;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.core.JobParameters;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.AwsS3BucketDetails;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.service.impl.instance.CloudToHarnessMappingServiceImpl;
import software.wings.settings.SettingValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class S3SyncEventWriterTest extends CategoryTest {
  @InjectMocks S3SyncEventWriter s3SyncEventWriter;
  @Mock private AwsS3SyncServiceImpl awsS3SyncService;
  @Mock JobParameters parameters;
  @Mock private CloudToHarnessMappingServiceImpl cloudToHarnessMappingService;

  private final String TEST_ACCOUNT_ID = "S3_SYNC_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String BILLING_ACCOUNT_ID = "S3_SYNC_BILLING_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String BILLING_BUCKET_PATH = "S3_SYNC_BILLING_BUCKET_PATH_" + this.getClass().getSimpleName();
  private final String BILLING_BUCKET_REGION = "S3_SYNC_BILLING_BUCKET_REGION_ID_" + this.getClass().getSimpleName();
  private final String EXTERNAL_ID = "EXTERNAL_ID" + this.getClass().getSimpleName();
  private final String ROLE_ARN = "ROLE_ARN" + this.getClass().getSimpleName();
  private final String MASTER = "MASTER";

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void setS3SyncEventWriterTest() {
    AwsCrossAccountAttributes awsCrossAccountAttributes =
        AwsCrossAccountAttributes.builder().externalId(EXTERNAL_ID).crossAccountRoleArn(ROLE_ARN).build();
    AwsS3BucketDetails s3BucketDetails =
        AwsS3BucketDetails.builder().s3BucketName(BILLING_BUCKET_PATH).region(BILLING_BUCKET_REGION).build();
    SettingValue settingValue = CEAwsConfig.builder()
                                    .s3BucketDetails(s3BucketDetails)
                                    .awsCrossAccountAttributes(awsCrossAccountAttributes)
                                    .awsAccountId(BILLING_ACCOUNT_ID)
                                    .awsAccountType(MASTER)
                                    .build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(TEST_ACCOUNT_ID)
                                            .withCategory(SettingAttribute.SettingCategory.CE_CONNECTOR)
                                            .withValue(settingValue)
                                            .build();
    Mockito.doNothing().when(awsS3SyncService).syncBuckets(any());
    Mockito.doReturn(Arrays.asList(settingAttribute))
        .when(cloudToHarnessMappingService)
        .getSettingAttributes(any(), any(), any());
    Mockito.doReturn(TEST_ACCOUNT_ID).when(parameters).getString(any());

    List<SettingAttribute> settingAttributeList = new ArrayList<>();
    s3SyncEventWriter.write(settingAttributeList);
    Collection<Invocation> invocations = Mockito.mockingDetails(awsS3SyncService).getInvocations();
    int numberOfCalls = invocations.size();
    assertThat(numberOfCalls).isEqualTo(1);
  }
}
