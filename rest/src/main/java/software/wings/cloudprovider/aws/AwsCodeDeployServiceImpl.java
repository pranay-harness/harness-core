package software.wings.cloudprovider.aws;

import static com.amazonaws.services.codedeploy.model.DeploymentStatus.Failed;
import static com.amazonaws.services.codedeploy.model.DeploymentStatus.Stopped;
import static com.amazonaws.services.codedeploy.model.DeploymentStatus.Succeeded;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INIT_TIMEOUT;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.CreateDeploymentResult;
import com.amazonaws.services.codedeploy.model.DeploymentGroupInfo;
import com.amazonaws.services.codedeploy.model.DeploymentInfo;
import com.amazonaws.services.codedeploy.model.ErrorInformation;
import com.amazonaws.services.codedeploy.model.GetDeploymentGroupRequest;
import com.amazonaws.services.codedeploy.model.GetDeploymentRequest;
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
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.CodeDeployDeploymentInfo;
import software.wings.exception.InvalidRequestException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 6/22/17.
 */
@Singleton
public class AwsCodeDeployServiceImpl implements AwsCodeDeployService {
  private static final Logger logger = LoggerFactory.getLogger(AwsCodeDeployServiceImpl.class);

  @Inject private AwsHelperService awsHelperService;
  @Inject private TimeLimiter timeLimiter;

  @Override
  public List<String> listApplications(
      String region, SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptionDetails) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting, encryptionDetails);

    List<String> applications = new ArrayList<>();
    ListApplicationsResult listApplicationsResult;
    ListApplicationsRequest listApplicationsRequest = new ListApplicationsRequest();
    do {
      listApplicationsResult =
          awsHelperService.listApplicationsResult(awsConfig, encryptionDetails, region, listApplicationsRequest);
      applications.addAll(listApplicationsResult.getApplications());
      listApplicationsRequest.setNextToken(listApplicationsResult.getNextToken());
    } while (listApplicationsRequest.getNextToken() != null);
    return applications;
  }

  @Override
  public List<String> listDeploymentGroup(String region, String appName, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting, encryptedDataDetails);

    List<String> deploymentGroups = new ArrayList<>();
    ListDeploymentGroupsResult listDeploymentGroupsResult;
    ListDeploymentGroupsRequest listDeploymentGroupsRequest =
        new ListDeploymentGroupsRequest().withApplicationName(appName);
    do {
      listDeploymentGroupsResult = awsHelperService.listDeploymentGroupsResult(
          awsConfig, encryptedDataDetails, region, listDeploymentGroupsRequest);
      deploymentGroups.addAll(listDeploymentGroupsResult.getDeploymentGroups());
      listDeploymentGroupsResult.setNextToken(listDeploymentGroupsResult.getNextToken());
    } while (listDeploymentGroupsResult.getNextToken() != null);
    return deploymentGroups;
  }

  @Override
  public List<String> listDeploymentConfiguration(
      String region, SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting, encryptedDataDetails);

    List<String> deploymentConfigurations = new ArrayList<>();
    ListDeploymentConfigsResult listDeploymentConfigsResult;
    ListDeploymentConfigsRequest listDeploymentConfigsRequest = new ListDeploymentConfigsRequest();
    do {
      listDeploymentConfigsResult = awsHelperService.listDeploymentConfigsResult(
          awsConfig, encryptedDataDetails, region, listDeploymentConfigsRequest);
      deploymentConfigurations.addAll(listDeploymentConfigsResult.getDeploymentConfigsList());
      listDeploymentConfigsRequest.setNextToken(listDeploymentConfigsResult.getNextToken());
    } while (listDeploymentConfigsRequest.getNextToken() != null);
    return deploymentConfigurations;
  }

  @Override
  public CodeDeployDeploymentInfo deployApplication(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, CreateDeploymentRequest createDeploymentRequest,
      ExecutionLogCallback executionLogCallback) {
    try {
      AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting, encryptedDataDetails);

      CreateDeploymentResult deploymentResult =
          awsHelperService.createCodeDeployDeployment(awsConfig, encryptedDataDetails, region, createDeploymentRequest);

      executionLogCallback.saveExecutionLog(
          format("Deployment started deployment id: [%s]", deploymentResult.getDeploymentId()), LogLevel.INFO);
      waitForDeploymentToComplete(awsConfig, encryptedDataDetails, region, deploymentResult.getDeploymentId());
      DeploymentInfo deploymentInfo =
          awsHelperService
              .getCodeDeployDeployment(awsConfig, encryptedDataDetails, region,
                  new GetDeploymentRequest().withDeploymentId(deploymentResult.getDeploymentId()))
              .getDeploymentInfo();
      String finalDeploymentStatus = deploymentInfo.getStatus();

      ErrorInformation errorInformation = deploymentInfo.getErrorInformation();
      if (errorInformation != null) {
        executionLogCallback.saveExecutionLog(
            format("Deployment Error: [%s] [%s]", errorInformation.getCode(), errorInformation.getMessage()),
            LogLevel.ERROR);
      }

      CodeDeployDeploymentInfo codeDeployDeploymentInfo = new CodeDeployDeploymentInfo();
      codeDeployDeploymentInfo.setStatus(Failed.name().equals(finalDeploymentStatus) ? CommandExecutionStatus.FAILURE
                                                                                     : CommandExecutionStatus.SUCCESS);

      List<String> instanceIds = fetchAllDeploymentInstances(
          awsConfig, encryptedDataDetails, region, deploymentResult.getDeploymentId(), null);
      DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceIds);
      List<Instance> instances =
          awsHelperService.describeEc2Instances(awsConfig, encryptedDataDetails, region, describeInstancesRequest)
              .getReservations()
              .stream()
              .flatMap(reservation -> reservation.getInstances().stream())
              .collect(toList());

      codeDeployDeploymentInfo.setInstances(instances);
      codeDeployDeploymentInfo.setDeploymentId(deploymentResult.getDeploymentId());
      return codeDeployDeploymentInfo;
    } catch (WingsException wex) {
      executionLogCallback.saveExecutionLog("Deployment failed ", LogLevel.ERROR);
      executionLogCallback.saveExecutionLog(
          wex.getCause() != null ? wex.getCause().getMessage() : (String) wex.getParams().get("message"),
          LogLevel.ERROR);
      throw wex;
    }
  }

  @Override
  public List<Instance> listDeploymentInstances(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String deploymentId) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting, encryptedDataDetails);

    List<String> instanceIds = fetchAllDeploymentInstances(
        awsConfig, encryptedDataDetails, region, deploymentId, asList(InstanceStatus.Succeeded.name()));

    if (isNotEmpty(instanceIds)) {
      DescribeInstancesRequest describeInstancesRequest =
          awsHelperService.getDescribeInstancesRequestWithRunningFilter().withInstanceIds(instanceIds);

      return awsHelperService.describeEc2Instances(awsConfig, encryptedDataDetails, region, describeInstancesRequest)
          .getReservations()
          .stream()
          .flatMap(reservation -> reservation.getInstances().stream())
          .collect(toList());
    } else {
      return Collections.EMPTY_LIST;
    }
  }

  public RevisionLocation getApplicationRevisionList(String region, String appName, String deploymentGroupName,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting, encryptedDataDetails);
    GetDeploymentGroupRequest getDeploymentGroupRequest =
        new GetDeploymentGroupRequest().withApplicationName(appName).withDeploymentGroupName(deploymentGroupName);
    DeploymentGroupInfo deploymentGroupInfo =
        awsHelperService
            .getCodeDeployDeploymentGroup(awsConfig, encryptedDataDetails, region, getDeploymentGroupRequest)
            .getDeploymentGroupInfo();
    return deploymentGroupInfo.getTargetRevision();
  }

  private List<String> fetchAllDeploymentInstances(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String region, String deploymentId, List<String> instanceStatusList) {
    List<String> instances = new ArrayList<>();
    ListDeploymentInstancesResult listDeploymentInstancesResult;
    ListDeploymentInstancesRequest listDeploymentInstancesRequest =
        new ListDeploymentInstancesRequest().withDeploymentId(deploymentId);
    if (isNotEmpty(instanceStatusList)) {
      listDeploymentInstancesRequest.withInstanceStatusFilter(instanceStatusList);
    }

    do {
      listDeploymentInstancesResult = awsHelperService.listDeploymentInstances(
          awsConfig, encryptedDataDetails, region, listDeploymentInstancesRequest);
      instances.addAll(listDeploymentInstancesResult.getInstancesList());
      listDeploymentInstancesRequest.setNextToken(listDeploymentInstancesResult.getNextToken());
    } while (listDeploymentInstancesResult.getNextToken() != null);
    return instances;
  }

  private void waitForDeploymentToComplete(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String region, String deploymentId) {
    try {
      Set<String> finalDeploymentStatus = Sets.newHashSet(Succeeded.name(), Failed.name(), Stopped.name());
      timeLimiter.callWithTimeout(() -> {
        while (!deploymentCompleted(awsConfig, encryptedDataDetails, region, deploymentId, finalDeploymentStatus)) {
          sleep(ofSeconds(10));
        }
        return true;
      }, 10L, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      throw new WingsException(INIT_TIMEOUT).addParam("message", "Timed out waiting for deployment to complete");
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException("Error while waiting for deployment to complete", e);
    }
  }

  private boolean deploymentCompleted(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String region, String deploymentId, Set<String> finalDeploymentStatus) {
    return finalDeploymentStatus.contains(awsHelperService
                                              .getCodeDeployDeployment(awsConfig, encryptedDataDetails, region,
                                                  new GetDeploymentRequest().withDeploymentId(deploymentId))
                                              .getDeploymentInfo()
                                              .getStatus());
  }
}
