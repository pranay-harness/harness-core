package software.wings.service.intfc.aws.delegate;

import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;

import java.util.List;

public interface AwsRoute53HelperServiceDelegate {
  List<AwsRoute53HostedZoneData> listHostedZones(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
  void upsertRoute53ParentRecord(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String parentRecordName, String parentRecordHostedZoneId, int blueServiceWeight, String blueServiceRecord,
      int greeServiceWeight, String greenServiceRecord, int ttl);
}