package software.wings.service.intfc.aws.manager;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import com.amazonaws.services.ecs.model.Service;
import java.util.List;

public interface AwsEcsHelperServiceManager {
  List<String> listClusters(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);

  List<Service> listClusterServices(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId, String cluster);
}
