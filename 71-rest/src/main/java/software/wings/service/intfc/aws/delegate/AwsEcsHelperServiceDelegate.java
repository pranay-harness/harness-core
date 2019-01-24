package software.wings.service.intfc.aws.delegate;

import com.amazonaws.services.ecs.model.Service;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface AwsEcsHelperServiceDelegate {
  List<String> listClusters(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
  List<Service> listServicesForCluster(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String cluster);
}