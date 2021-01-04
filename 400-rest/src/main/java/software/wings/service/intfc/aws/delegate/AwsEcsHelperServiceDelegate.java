package software.wings.service.intfc.aws.delegate;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.ContainerInstanceStatus;
import com.amazonaws.services.ecs.model.DesiredStatus;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.Task;
import java.util.List;

public interface AwsEcsHelperServiceDelegate {
  List<String> listClusters(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
  List<Service> listServicesForCluster(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String cluster);
  List<String> listTasksArnForService(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String cluster, String service, DesiredStatus desiredStatus);
  List<Task> listTasksForService(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String cluster, String service, DesiredStatus desiredStatus);
  List<ContainerInstance> listContainerInstancesForCluster(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String cluster,
      ContainerInstanceStatus containerInstanceStatus);
}
