package software.wings.service.impl.aws.delegate;

import static com.amazonaws.services.ecs.model.ServiceField.TAGS;
import static com.google.common.collect.Lists.newArrayList;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.ContainerInstanceField;
import com.amazonaws.services.ecs.model.ContainerInstanceStatus;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesResult;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.DesiredStatus;
import com.amazonaws.services.ecs.model.ListClustersRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import com.amazonaws.services.ecs.model.ListContainerInstancesRequest;
import com.amazonaws.services.ecs.model.ListContainerInstancesResult;
import com.amazonaws.services.ecs.model.ListServicesRequest;
import com.amazonaws.services.ecs.model.ListServicesResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.ListTasksResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.Task;
import com.amazonaws.services.ecs.model.TaskField;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class AwsEcsHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsEcsHelperServiceDelegate {
  @VisibleForTesting
  AmazonECSClient getAmazonEcsClient(String region, AwsConfig awsConfig) {
    AmazonECSClientBuilder builder = AmazonECSClientBuilder.standard().withRegion(region);
    attachCredentials(builder, awsConfig);
    return (AmazonECSClient) builder.build();
  }

  private String getIdFromArn(String arn) {
    return arn.substring(arn.lastIndexOf('/') + 1);
  }

  @Override
  public List<String> listClusters(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      List<String> result = new ArrayList<>();
      String nextToken = null;
      do {
        ListClustersRequest listClustersRequest = new ListClustersRequest().withNextToken(nextToken);
        tracker.trackECSCall("List Clusters");
        ListClustersResult listClustersResult = getAmazonEcsClient(region, awsConfig).listClusters(listClustersRequest);
        result.addAll(listClustersResult.getClusterArns().stream().map(this ::getIdFromArn).collect(toList()));
        nextToken = listClustersResult.getNextToken();
      } while (nextToken != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public List<Service> listServicesForCluster(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String cluster) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      AmazonECSClient client = getAmazonEcsClient(region, awsConfig);
      List<String> serviceArns = newArrayList();
      String nextToken = null;
      do {
        ListServicesRequest listServicesRequest =
            new ListServicesRequest().withCluster(cluster).withNextToken(nextToken);
        tracker.trackECSCall("List Services");
        ListServicesResult listServicesResult = client.listServices(listServicesRequest);
        List<String> arnsBatch = listServicesResult.getServiceArns();
        if (isNotEmpty(arnsBatch)) {
          serviceArns.addAll(arnsBatch);
        }
        nextToken = listServicesResult.getNextToken();
      } while (nextToken != null);
      int counter = 0;
      List<Service> allServices = newArrayList();
      while (counter < serviceArns.size()) {
        // We can ONLY describe 10 services at a time.
        List<String> arnsBatch = newArrayList();
        for (int i = 0; i < 10 && counter < serviceArns.size(); i++) {
          arnsBatch.add(serviceArns.get(counter));
          counter++;
        }
        DescribeServicesRequest describeServicesRequest =
            new DescribeServicesRequest().withCluster(cluster).withServices(arnsBatch).withInclude(TAGS);
        tracker.trackECSCall("Describe Services");
        DescribeServicesResult describeServicesResult = client.describeServices(describeServicesRequest);
        allServices.addAll(describeServicesResult.getServices());
      }
      return allServices;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public List<String> listTasksArnForService(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String cluster, String service, DesiredStatus desiredStatus) {
    List<String> taskArns = newArrayList();
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      AmazonECSClient client = getAmazonEcsClient(region, awsConfig);
      String nextToken = null;
      do {
        ListTasksRequest listTasksRequest =
            new ListTasksRequest().withCluster(cluster).withDesiredStatus(desiredStatus).withNextToken(nextToken);
        if (null != service) {
          listTasksRequest.withServiceName(service);
        }
        tracker.trackECSCall("List Tasks");
        ListTasksResult listTasksResult = client.listTasks(listTasksRequest);
        List<String> arnsBatch = listTasksResult.getTaskArns();
        if (isNotEmpty(arnsBatch)) {
          taskArns.addAll(arnsBatch);
        }
        nextToken = listTasksResult.getNextToken();
      } while (nextToken != null);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return taskArns;
  }

  @Override
  public List<Task> listTasksForService(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String cluster, String service, DesiredStatus desiredStatus) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      AmazonECSClient client = getAmazonEcsClient(region, awsConfig);
      List<String> taskArns =
          listTasksArnForService(awsConfig, encryptionDetails, region, cluster, service, desiredStatus);
      int counter = 0;
      List<Task> allTasks = newArrayList();
      while (counter < taskArns.size()) {
        // We can ONLY describe 100 tasks at a time.
        List<String> arnsBatch = newArrayList();
        for (int i = 0; i < 100 && counter < taskArns.size(); i++) {
          arnsBatch.add(taskArns.get(counter));
          counter++;
        }
        DescribeTasksRequest describeTasksRequest =
            new DescribeTasksRequest().withCluster(cluster).withTasks(arnsBatch).withInclude(TaskField.TAGS);
        tracker.trackECSCall("Describe Tasks");
        DescribeTasksResult describeTasksResult = client.describeTasks(describeTasksRequest);
        allTasks.addAll(describeTasksResult.getTasks());
      }
      return allTasks;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public List<ContainerInstance> listContainerInstancesForCluster(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String cluster,
      ContainerInstanceStatus containerInstanceStatus) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      AmazonECSClient client = getAmazonEcsClient(region, awsConfig);
      List<String> containerInstanceArns = newArrayList();
      String nextToken = null;
      do {
        ListContainerInstancesRequest listContainerInstancesRequest = new ListContainerInstancesRequest()
                                                                          .withCluster(cluster)
                                                                          .withStatus(containerInstanceStatus)
                                                                          .withNextToken(nextToken);
        tracker.trackECSCall("List Container Instances");
        ListContainerInstancesResult listContainerInstancesResult =
            client.listContainerInstances(listContainerInstancesRequest);
        List<String> arnsBatch = listContainerInstancesResult.getContainerInstanceArns();
        if (isNotEmpty(arnsBatch)) {
          containerInstanceArns.addAll(arnsBatch);
        }
        nextToken = listContainerInstancesResult.getNextToken();
      } while (nextToken != null);

      int counter = 0;
      List<ContainerInstance> allContainerInstance = newArrayList();
      while (counter < containerInstanceArns.size()) {
        // We can ONLY describe 100 container instances at a time.
        List<String> arnsBatch = newArrayList();
        for (int i = 0; i < 100 && counter < containerInstanceArns.size(); i++) {
          arnsBatch.add(containerInstanceArns.get(counter));
          counter++;
        }
        DescribeContainerInstancesRequest describeContainerInstancesRequest =
            new DescribeContainerInstancesRequest().withCluster(cluster).withContainerInstances(arnsBatch).withInclude(
                ContainerInstanceField.TAGS);
        tracker.trackECSCall("Describe Containers");
        DescribeContainerInstancesResult describeContainerInstancesResult =
            client.describeContainerInstances(describeContainerInstancesRequest);
        allContainerInstance.addAll(describeContainerInstancesResult.getContainerInstances());
      }
      return allContainerInstance;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }
}
