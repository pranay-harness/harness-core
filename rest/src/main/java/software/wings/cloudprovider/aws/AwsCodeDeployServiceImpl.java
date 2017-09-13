package software.wings.cloudprovider.aws;

import static com.amazonaws.services.codedeploy.model.DeploymentStatus.Failed;
import static com.amazonaws.services.codedeploy.model.DeploymentStatus.Stopped;
import static com.amazonaws.services.codedeploy.model.DeploymentStatus.Succeeded;
import static software.wings.beans.ErrorCode.INIT_TIMEOUT;

import com.google.common.collect.Sets;

import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.CreateDeploymentResult;
import com.amazonaws.services.codedeploy.model.DeploymentGroupInfo;
import com.amazonaws.services.codedeploy.model.DeploymentInfo;
import com.amazonaws.services.codedeploy.model.ErrorInformation;
import com.amazonaws.services.codedeploy.model.GetDeploymentGroupRequest;
import com.amazonaws.services.codedeploy.model.GetDeploymentRequest;
import com.amazonaws.services.codedeploy.model.ListApplicationsRequest;
import com.amazonaws.services.codedeploy.model.ListApplicationsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesResult;
import com.amazonaws.services.codedeploy.model.RevisionLocation;
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

    List<String> applications = new ArrayList<>();
    ListApplicationsResult listApplicationsResult;
    ListApplicationsRequest listApplicationsRequest = new ListApplicationsRequest();
    do {
      listApplicationsResult = awsHelperService.listApplicationsResult(awsConfig, region, listApplicationsRequest);
      applications.addAll(listApplicationsResult.getApplications());
      listApplicationsRequest.setNextToken(listApplicationsResult.getNextToken());
    } while (listApplicationsRequest.getNextToken() != null);
    return applications;
  }

  @Override
  public List<String> listDeploymentGroup(String region, String appName, SettingAttribute cloudProviderSetting) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting);

    List<String> deploymentGroups = new ArrayList<>();
    ListDeploymentGroupsResult listDeploymentGroupsResult;
    ListDeploymentGroupsRequest listDeploymentGroupsRequest =
        new ListDeploymentGroupsRequest().withApplicationName(appName);
    do {
      listDeploymentGroupsResult =
          awsHelperService.listDeploymentGroupsResult(awsConfig, region, listDeploymentGroupsRequest);
      deploymentGroups.addAll(listDeploymentGroupsResult.getDeploymentGroups());
      listDeploymentGroupsResult.setNextToken(listDeploymentGroupsResult.getNextToken());
    } while (listDeploymentGroupsResult.getNextToken() != null);
    return deploymentGroups;
  }

  @Override
  public List<String> listDeploymentConfiguration(String region, SettingAttribute cloudProviderSetting) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting);

    List<String> deploymentConfigurations = new ArrayList<>();
    ListDeploymentConfigsResult listDeploymentConfigsResult;
    ListDeploymentConfigsRequest listDeploymentConfigsRequest = new ListDeploymentConfigsRequest();
    do {
      listDeploymentConfigsResult =
          awsHelperService.listDeploymentConfigsResult(awsConfig, region, listDeploymentConfigsRequest);
      deploymentConfigurations.addAll(listDeploymentConfigsResult.getDeploymentConfigsList());
      listDeploymentConfigsRequest.setNextToken(listDeploymentConfigsResult.getNextToken());
    } while (listDeploymentConfigsRequest.getNextToken() != null);
    return deploymentConfigurations;
  }

  @Override
  public CodeDeployDeploymentInfo deployApplication(String region, SettingAttribute cloudProviderSetting,
      CreateDeploymentRequest createDeploymentRequest, ExecutionLogCallback executionLogCallback) {
    try {
      AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting);

      CreateDeploymentResult deploymentResult =
          awsHelperService.createCodeDeployDeployment(awsConfig, region, createDeploymentRequest);

      executionLogCallback.saveExecutionLog(
          String.format("Deployment started deployment id: [%s]", deploymentResult.getDeploymentId()), LogLevel.INFO);
      waitForDeploymentToComplete(awsConfig, region, deploymentResult.getDeploymentId());
      DeploymentInfo deploymentInfo =
          awsHelperService
              .getCodeDeployDeployment(
                  awsConfig, region, new GetDeploymentRequest().withDeploymentId(deploymentResult.getDeploymentId()))
              .getDeploymentInfo();
      String finalDeploymentStatus = deploymentInfo.getStatus();

      ErrorInformation errorInformation = deploymentInfo.getErrorInformation();
      if (errorInformation != null) {
        executionLogCallback.saveExecutionLog(
            String.format("Deployment Error: [%s] [%s]", errorInformation.getCode(), errorInformation.getMessage()),
            LogLevel.ERROR);
      }

      CodeDeployDeploymentInfo codeDeployDeploymentInfo = new CodeDeployDeploymentInfo();
      codeDeployDeploymentInfo.setStatus(Failed.name().equals(finalDeploymentStatus) ? CommandExecutionStatus.FAILURE
                                                                                     : CommandExecutionStatus.SUCCESS);

      List<String> instanceIds = fetchAllDeploymentInstances(awsConfig, region, deploymentResult.getDeploymentId());
      DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceIds);
      List<Instance> instances = awsHelperService.describeEc2Instances(awsConfig, region, describeInstancesRequest)
                                     .getReservations()
                                     .stream()
                                     .flatMap(reservation -> reservation.getInstances().stream())
                                     .collect(Collectors.toList());

      codeDeployDeploymentInfo.setInstances(instances);
      return codeDeployDeploymentInfo;
    } catch (WingsException wex) {
      executionLogCallback.saveExecutionLog("Deployment failed ", LogLevel.ERROR);
      executionLogCallback.saveExecutionLog(
          wex.getCause() != null ? wex.getCause().getMessage() : (String) wex.getParams().get("message"),
          LogLevel.ERROR);
      throw wex;
    }
  }

  public RevisionLocation getApplicationRevisionList(
      String region, String appName, String deploymentGroupName, SettingAttribute cloudProviderSetting) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting);
    GetDeploymentGroupRequest getDeploymentGroupRequest =
        new GetDeploymentGroupRequest().withApplicationName(appName).withDeploymentGroupName(deploymentGroupName);
    DeploymentGroupInfo deploymentGroupInfo =
        awsHelperService.getCodeDeployDeploymentGroup(awsConfig, region, getDeploymentGroupRequest)
            .getDeploymentGroupInfo();
    return deploymentGroupInfo.getTargetRevision();
  }

  private List<String> fetchAllDeploymentInstances(AwsConfig awsConfig, String region, String deploymentId) {
    List<String> instances = new ArrayList<>();
    ListDeploymentInstancesResult listDeploymentInstancesResult;
    ListDeploymentInstancesRequest listDeploymentInstancesRequest =
        new ListDeploymentInstancesRequest().withDeploymentId(deploymentId);
    do {
      listDeploymentInstancesResult =
          awsHelperService.listDeploymentInstances(awsConfig, region, listDeploymentInstancesRequest);
      instances.addAll(listDeploymentInstancesResult.getInstancesList());
      listDeploymentInstancesResult.setNextToken(listDeploymentInstancesResult.getNextToken());
    } while (listDeploymentInstancesResult.getNextToken() != null);
    return instances;
  }

  private void waitForDeploymentToComplete(AwsConfig awsConfig, String region, String deploymentId) {
    int retryCount = RETRY_COUNTER;
    Set<String> finalDeploymentStatus = Sets.newHashSet(Succeeded.name(), Failed.name(), Stopped.name());
    while (!deploymentCompleted(awsConfig, region, deploymentId, finalDeploymentStatus)) {
      if (retryCount-- <= 0) {
        throw new WingsException(INIT_TIMEOUT, "message", "All instances didn't registered with cluster");
      }
      Misc.quietSleep(SLEEP_INTERVAL);
    }
  }

  private boolean deploymentCompleted(
      AwsConfig awsConfig, String region, String deploymentId, Set<String> finalDeploymentStatus) {
    return finalDeploymentStatus.contains(
        awsHelperService
            .getCodeDeployDeployment(awsConfig, region, new GetDeploymentRequest().withDeploymentId(deploymentId))
            .getDeploymentInfo()
            .getStatus());
  }
}
