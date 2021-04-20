package software.wings.service.intfc.aws.manager;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import com.amazonaws.services.ecs.model.Service;
import java.util.List;

@OwnedBy(CDP)
public interface AwsEcsHelperServiceManager {
  List<String> listClusters(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);

  List<Service> listClusterServices(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId, String cluster);
}
