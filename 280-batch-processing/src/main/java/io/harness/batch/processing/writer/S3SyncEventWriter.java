package io.harness.batch.processing.writer;

import static software.wings.beans.SettingAttribute.SettingCategory.CE_CONNECTOR;
import static software.wings.settings.SettingVariableTypes.CE_AWS;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.ccm.S3SyncRecord;
import io.harness.batch.processing.ccm.S3SyncRecord.S3SyncRecordBuilder;
import io.harness.batch.processing.service.impl.AwsS3SyncServiceImpl;

import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.AwsS3BucketDetails;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;

import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Singleton
public class S3SyncEventWriter extends EventWriter implements ItemWriter<SettingAttribute> {
  @Autowired private AwsS3SyncServiceImpl awsS3SyncService;
  private JobParameters parameters;
  private static final String MASTER = "MASTER";

  @BeforeStep
  public void beforeStep(final StepExecution stepExecution) {
    parameters = stepExecution.getJobExecution().getJobParameters();
  }

  @Override
  public void write(List<? extends SettingAttribute> dummySettingAttributeList) {
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);

    List<SettingAttribute> ceConnectorsList =
        cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(accountId, CE_CONNECTOR, CE_AWS);

    log.info("Processing batch size of {} in S3SyncEventWriter", ceConnectorsList.size());

    ceConnectorsList.forEach(settingAttribute -> {
      S3SyncRecordBuilder s3SyncRecordBuilder = S3SyncRecord.builder();
      s3SyncRecordBuilder.accountId(settingAttribute.getAccountId());
      s3SyncRecordBuilder.settingId(settingAttribute.getUuid());

      if (settingAttribute.getValue() instanceof CEAwsConfig
          && (((CEAwsConfig) settingAttribute.getValue()).getAwsAccountType()).equals(MASTER)
          && ((CEAwsConfig) settingAttribute.getValue()).getAwsCrossAccountAttributes() != null) {
        AwsCrossAccountAttributes awsCrossAccountAttributes =
            ((CEAwsConfig) settingAttribute.getValue()).getAwsCrossAccountAttributes();
        CEAwsConfig ceAwsConfig = (CEAwsConfig) settingAttribute.getValue();
        AwsS3BucketDetails s3BucketDetails = ceAwsConfig.getS3BucketDetails();
        s3SyncRecordBuilder.billingAccountId(ceAwsConfig.getAwsMasterAccountId());
        s3SyncRecordBuilder.curReportName(ceAwsConfig.getCurReportName());
        s3SyncRecordBuilder.billingBucketPath(String.join("/", "s3://" + s3BucketDetails.getS3BucketName(),
            s3BucketDetails.getS3Prefix(), ceAwsConfig.getCurReportName()));
        s3SyncRecordBuilder.billingBucketRegion(s3BucketDetails.getRegion());
        s3SyncRecordBuilder.externalId(awsCrossAccountAttributes.getExternalId());
        s3SyncRecordBuilder.roleArn(awsCrossAccountAttributes.getCrossAccountRoleArn());
        awsS3SyncService.syncBuckets(s3SyncRecordBuilder.build());
      }
    });
  }
}
