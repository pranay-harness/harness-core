package io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc;

import software.wings.beans.AwsCrossAccountAttributes;

import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.ContainerInstanceStatus;
import com.amazonaws.services.ecs.model.DesiredStatus;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.Task;
import java.util.List;

public interface AwsECSHelperService {
  List<String> listECSClusters(String region, AwsCrossAccountAttributes awsCrossAccountAttributes);

  List<Service> listServicesForCluster(
      AwsCrossAccountAttributes awsCrossAccountAttributes, String region, String cluster);

  List<ContainerInstance> listContainerInstancesForCluster(AwsCrossAccountAttributes awsCrossAccountAttributes,
      String region, String cluster, ContainerInstanceStatus containerInstanceStatus);

  List<String> listTasksArnForService(AwsCrossAccountAttributes awsCrossAccountAttributes, String region,
      String cluster, String service, DesiredStatus desiredStatus);

  List<Task> listTasksForService(AwsCrossAccountAttributes awsCrossAccountAttributes, String region, String cluster,
      String service, DesiredStatus desiredStatus);
}
