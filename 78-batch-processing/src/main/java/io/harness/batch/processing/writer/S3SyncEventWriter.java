package io.harness.batch.processing.writer;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.batch.processing.ccm.S3SyncRecord;
import io.harness.batch.processing.ccm.S3SyncRecord.S3SyncRecordBuilder;
import io.harness.batch.processing.service.impl.AwsS3SyncServiceImpl;
import io.harness.ccm.BillingReportConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;

import java.util.List;

@Slf4j
@Singleton
public class S3SyncEventWriter implements ItemWriter<SettingAttribute> {
  @Inject AwsS3SyncServiceImpl awsS3SyncService;

  @Override
  public void write(List<? extends SettingAttribute> settingAttributes) {
    logger.info("Published batch size is S3SyncEventWriter {} ", settingAttributes.size());
    settingAttributes.forEach(settingAttribute -> {
      S3SyncRecordBuilder s3SyncRecordBuilder = S3SyncRecord.builder();
      s3SyncRecordBuilder.accountId(settingAttribute.getAccountId());
      s3SyncRecordBuilder.settingId(settingAttribute.getUuid());

      if (settingAttribute.getValue() instanceof AwsConfig
          && ((AwsConfig) settingAttribute.getValue()).getCcmConfig().getBillingReportConfig() != null) {
        BillingReportConfig billingReportConfig =
            ((AwsConfig) settingAttribute.getValue()).getCcmConfig().getBillingReportConfig();
        s3SyncRecordBuilder.billingAccountId(billingReportConfig.getBillingAccountId());
        s3SyncRecordBuilder.billingBucketPath(billingReportConfig.getBillingBucketPath());
        s3SyncRecordBuilder.billingBucketRegion(billingReportConfig.getBillingBucketRegion());
        s3SyncRecordBuilder.externalId(billingReportConfig.getExternalId());
        s3SyncRecordBuilder.roleArn(billingReportConfig.getRoleArn());
        awsS3SyncService.syncBuckets(s3SyncRecordBuilder.build());
      }
    });
  }
}
