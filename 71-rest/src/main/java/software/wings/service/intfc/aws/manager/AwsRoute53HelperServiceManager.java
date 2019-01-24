package software.wings.service.intfc.aws.manager;

import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;

import java.util.List;

public interface AwsRoute53HelperServiceManager {
  List<AwsRoute53HostedZoneData> listHostedZones(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);
}