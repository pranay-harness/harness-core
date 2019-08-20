package software.wings.service.impl.aws.delegate;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClientBuilder;
import com.amazonaws.services.codedeploy.model.DeploymentGroupInfo;
import com.amazonaws.services.codedeploy.model.GetDeploymentGroupRequest;
import com.amazonaws.services.codedeploy.model.GetDeploymentGroupResult;
import com.amazonaws.services.codedeploy.model.InstanceStatus;
import com.amazonaws.services.codedeploy.model.ListApplicationsRequest;
import com.amazonaws.services.codedeploy.model.ListApplicationsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesResult;
import com.amazonaws.services.codedeploy.model.RevisionLocation;
import com.amazonaws.services.codedeploy.model.S3Location;
import com.amazonaws.services.ec2.model.Instance;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.service.impl.AwsUtils;
import software.wings.service.impl.aws.model.AwsCodeDeployS3LocationData;
import software.wings.service.intfc.aws.delegate.AwsCodeDeployHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Singleton
public class AwsCodeDeployHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsCodeDeployHelperServiceDelegate {
  @Inject private AwsUtils awsUtils;
  @Inject private AwsEc2HelperServiceDelegate awsEc2HelperServiceDelegate;

  @VisibleForTesting
  AmazonCodeDeployClient getAmazonCodeDeployClient(Regions region, AwsConfig awsConfig) {
    AmazonCodeDeployClientBuilder builder = AmazonCodeDeployClientBuilder.standard().withRegion(region);
    attachCredentials(builder, awsConfig);
    return (AmazonCodeDeployClient) builder.build();
  }

  @Override
  public List<String> listApplications(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      List<String> applications = new ArrayList<>();
      String nextToken = null;
      ListApplicationsResult listApplicationsResult;
      ListApplicationsRequest listApplicationsRequest;
      do {
        listApplicationsRequest = new ListApplicationsRequest().withNextToken(nextToken);
        listApplicationsResult =
            getAmazonCodeDeployClient(Regions.fromName(region), awsConfig).listApplications(listApplicationsRequest);
        applications.addAll(listApplicationsResult.getApplications());
        nextToken = listApplicationsResult.getNextToken();
      } while (nextToken != null);
      return applications;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public List<String> listDeploymentConfiguration(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptedDataDetails);
      String nextToken = null;
      List<String> deploymentConfigurations = new ArrayList<>();
      ListDeploymentConfigsResult listDeploymentConfigsResult;
      ListDeploymentConfigsRequest listDeploymentConfigsRequest;
      do {
        listDeploymentConfigsRequest = new ListDeploymentConfigsRequest().withNextToken(nextToken);
        listDeploymentConfigsResult = getAmazonCodeDeployClient(Regions.fromName(region), awsConfig)
                                          .listDeploymentConfigs(listDeploymentConfigsRequest);
        deploymentConfigurations.addAll(listDeploymentConfigsResult.getDeploymentConfigsList());
        nextToken = listDeploymentConfigsResult.getNextToken();
      } while (nextToken != null);
      return deploymentConfigurations;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public List<String> listDeploymentGroups(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String region, String appName) {
    try {
      encryptionService.decrypt(awsConfig, encryptedDataDetails);
      String nextToken = null;
      List<String> deploymentGroups = new ArrayList<>();
      ListDeploymentGroupsResult listDeploymentGroupsResult;
      ListDeploymentGroupsRequest listDeploymentGroupsRequest;
      do {
        listDeploymentGroupsRequest =
            new ListDeploymentGroupsRequest().withNextToken(nextToken).withApplicationName(appName);
        listDeploymentGroupsResult = getAmazonCodeDeployClient(Regions.fromName(region), awsConfig)
                                         .listDeploymentGroups(listDeploymentGroupsRequest);
        deploymentGroups.addAll(listDeploymentGroupsResult.getDeploymentGroups());
        nextToken = listDeploymentGroupsResult.getNextToken();
      } while (nextToken != null);
      return deploymentGroups;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public List<Instance> listDeploymentInstances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String region, String deploymentId) {
    try {
      encryptionService.decrypt(awsConfig, encryptedDataDetails);
      String nextToken = null;
      List<String> instanceIds = new ArrayList<>();
      ListDeploymentInstancesRequest listDeploymentInstancesRequest;
      ListDeploymentInstancesResult listDeploymentInstancesResult;
      AmazonCodeDeployClient amazonCodeDeployClient = getAmazonCodeDeployClient(Regions.fromName(region), awsConfig);
      do {
        listDeploymentInstancesRequest = new ListDeploymentInstancesRequest()
                                             .withNextToken(nextToken)
                                             .withDeploymentId(deploymentId)
                                             .withInstanceStatusFilter(asList(InstanceStatus.Succeeded.name()));
        listDeploymentInstancesResult = amazonCodeDeployClient.listDeploymentInstances(listDeploymentInstancesRequest);
        instanceIds.addAll(listDeploymentInstancesResult.getInstancesList());
        nextToken = listDeploymentInstancesResult.getNextToken();
      } while (nextToken != null);
      if (isNotEmpty(instanceIds)) {
        Set<String> instancesIdsSet = Sets.newHashSet(instanceIds);
        List<Instance> runningInstances =
            awsEc2HelperServiceDelegate.listEc2Instances(awsConfig, encryptedDataDetails, region,
                awsUtils.getAwsFilters(AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping().build(), null));
        List<Instance> result = new ArrayList<>();
        runningInstances.forEach(instance -> {
          if (instancesIdsSet.contains(instance.getInstanceId())) {
            result.add(instance);
          }
        });
        return result;
      }
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public AwsCodeDeployS3LocationData listAppRevision(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String region, String appName, String deploymentGroupName) {
    try {
      encryptionService.decrypt(awsConfig, encryptedDataDetails);
      GetDeploymentGroupRequest getDeploymentGroupRequest =
          new GetDeploymentGroupRequest().withApplicationName(appName).withDeploymentGroupName(deploymentGroupName);
      GetDeploymentGroupResult getDeploymentGroupResult =
          getAmazonCodeDeployClient(Regions.fromName(region), awsConfig).getDeploymentGroup(getDeploymentGroupRequest);
      DeploymentGroupInfo deploymentGroupInfo = getDeploymentGroupResult.getDeploymentGroupInfo();
      RevisionLocation revisionLocation = deploymentGroupInfo.getTargetRevision();
      if (revisionLocation == null || revisionLocation.getS3Location() == null) {
        return null;
      }
      S3Location s3Location = revisionLocation.getS3Location();
      return AwsCodeDeployS3LocationData.builder()
          .bucket(s3Location.getBucket())
          .key(s3Location.getKey())
          .bundleType(s3Location.getBundleType())
          .build();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return null;
  }
}