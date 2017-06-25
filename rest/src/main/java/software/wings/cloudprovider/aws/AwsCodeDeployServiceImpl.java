package software.wings.cloudprovider.aws;

import static com.amazonaws.services.codedeploy.model.DeploymentStatus.Failed;
import static com.amazonaws.services.codedeploy.model.DeploymentStatus.Stopped;
import static com.amazonaws.services.codedeploy.model.DeploymentStatus.Succeeded;
import static software.wings.beans.ErrorCode.INIT_TIMEOUT;

import com.google.common.collect.Sets;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.CreateDeploymentResult;
import com.amazonaws.services.codedeploy.model.GetDeploymentRequest;
import com.amazonaws.services.codedeploy.model.ListApplicationsRequest;
import com.amazonaws.services.codedeploy.model.ListApplicationsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.CodeDeployDeploymentInfo;
import software.wings.exception.WingsException;
import software.wings.service.impl.AwsHelperService;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 6/22/17.
 */
@Singleton
public class AwsCodeDeployServiceImpl implements AwsCodeDeployService {
  private static final int SLEEP_INTERVAL = 10 * 1000;
  private static final int RETRY_COUNTER = (10 * 60 * 1000) / SLEEP_INTERVAL; // 10 minutes
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private AwsHelperService awsHelperService;

  @Override
  public List<String> listApplications(String region, SettingAttribute cloudProviderSetting) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting);
    AmazonCodeDeployClient amazonCodeDeployClient = awsHelperService.getAmazonCodeDeployClient(
        Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());

    List<String> applications = new ArrayList<>();
    ListApplicationsResult listApplicationsResult;
    ListApplicationsRequest listApplicationsRequest = new ListApplicationsRequest();
    do {
      listApplicationsResult = amazonCodeDeployClient.listApplications(listApplicationsRequest);
      applications.addAll(listApplicationsResult.getApplications());
      listApplicationsRequest.setNextToken(listApplicationsResult.getNextToken());
    } while (listApplicationsRequest.getNextToken() != null);
    return applications;
  }

  @Override
  public List<String> listDeploymentGroup(String region, String appName, SettingAttribute cloudProviderSetting) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting);
    AmazonCodeDeployClient amazonCodeDeployClient = awsHelperService.getAmazonCodeDeployClient(
        Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());

    List<String> deploymentGroups = new ArrayList<>();
    ListDeploymentGroupsResult listDeploymentGroupsResult;
    ListDeploymentGroupsRequest listDeploymentGroupsRequest =
        new ListDeploymentGroupsRequest().withApplicationName(appName);
    do {
      listDeploymentGroupsResult = amazonCodeDeployClient.listDeploymentGroups(listDeploymentGroupsRequest);
      deploymentGroups.addAll(listDeploymentGroupsResult.getDeploymentGroups());
      listDeploymentGroupsResult.setNextToken(listDeploymentGroupsResult.getNextToken());
    } while (listDeploymentGroupsResult.getNextToken() != null);
    return deploymentGroups;
  }

  @Override
  public List<String> listDeploymentConfiguration(String region, SettingAttribute cloudProviderSetting) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting);
    AmazonCodeDeployClient amazonCodeDeployClient = awsHelperService.getAmazonCodeDeployClient(
        Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());

    List<String> deploymentConfigurations = new ArrayList<>();
    ListDeploymentConfigsResult listDeploymentConfigsResult;
    ListDeploymentConfigsRequest listDeploymentConfigsRequest = new ListDeploymentConfigsRequest();
    do {
      listDeploymentConfigsResult = amazonCodeDeployClient.listDeploymentConfigs(listDeploymentConfigsRequest);
      deploymentConfigurations.addAll(listDeploymentConfigsResult.getDeploymentConfigsList());
      listDeploymentConfigsRequest.setNextToken(listDeploymentConfigsResult.getNextToken());
    } while (listDeploymentConfigsRequest.getNextToken() != null);
    return deploymentConfigurations;
  }

  @Override
  public CodeDeployDeploymentInfo deployApplication(String region, SettingAttribute cloudProviderSetting,
      CreateDeploymentRequest createDeploymentRequest, ExecutionLogCallback executionLogCallback) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting);
    AmazonCodeDeployClient amazonCodeDeployClient = awsHelperService.getAmazonCodeDeployClient(
        Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
    CreateDeploymentResult deploymentResult = amazonCodeDeployClient.createDeployment(createDeploymentRequest);

    executionLogCallback.saveExecutionLog(
        String.format("Deployment started deployment id: [%s]", deploymentResult.getDeploymentId()), LogLevel.INFO);
    waitForDeploymentToComplete(deploymentResult.getDeploymentId(), amazonCodeDeployClient);
    String finalDeploymentStatus =
        amazonCodeDeployClient
            .getDeployment(new GetDeploymentRequest().withDeploymentId(deploymentResult.getDeploymentId()))
            .getDeploymentInfo()
            .getStatus();
    CodeDeployDeploymentInfo codeDeployDeploymentInfo = new CodeDeployDeploymentInfo();
    codeDeployDeploymentInfo.setStatus(
        Failed.name().equals(finalDeploymentStatus) ? CommandExecutionStatus.FAILURE : CommandExecutionStatus.SUCCESS);

    List<String> instanceIds = fetchAllDeploymentInstances(amazonCodeDeployClient, deploymentResult.getDeploymentId());
    List<Instance> instances =
        awsHelperService.getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
            .describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIds))
            .getReservations()
            .stream()
            .flatMap(reservation -> reservation.getInstances().stream())
            .collect(Collectors.toList());

    codeDeployDeploymentInfo.setInstances(instances);
    return codeDeployDeploymentInfo;
  }

  private List<String> fetchAllDeploymentInstances(AmazonCodeDeployClient amazonCodeDeployClient, String deploymentId) {
    List<String> instances = new ArrayList<>();
    ListDeploymentInstancesResult listDeploymentInstancesResult;
    ListDeploymentInstancesRequest listDeploymentInstancesRequest =
        new ListDeploymentInstancesRequest().withDeploymentId(deploymentId);
    do {
      listDeploymentInstancesResult = amazonCodeDeployClient.listDeploymentInstances(listDeploymentInstancesRequest);
      instances.addAll(listDeploymentInstancesResult.getInstancesList());
      listDeploymentInstancesResult.setNextToken(listDeploymentInstancesResult.getNextToken());
    } while (listDeploymentInstancesResult.getNextToken() != null);
    return instances;
  }

  private void waitForDeploymentToComplete(String deploymentId, AmazonCodeDeployClient amazonCodeDeployClient) {
    int retryCount = RETRY_COUNTER;
    Set<String> finalDeploymentStatus = Sets.newHashSet(Succeeded.name(), Failed.name(), Stopped.name());
    while (!deploymentCompleted(deploymentId, amazonCodeDeployClient, finalDeploymentStatus)) {
      if (retryCount-- <= 0) {
        throw new WingsException(INIT_TIMEOUT, "message", "All instances didn't registered with cluster");
      }
      Misc.quietSleep(SLEEP_INTERVAL);
    }
  }

  private boolean deploymentCompleted(
      String deploymentId, AmazonCodeDeployClient amazonCodeDeployClient, Set<String> finalDeploymentStatus) {
    return finalDeploymentStatus.contains(
        amazonCodeDeployClient.getDeployment(new GetDeploymentRequest().withDeploymentId(deploymentId))
            .getDeploymentInfo()
            .getStatus());
  }
}
