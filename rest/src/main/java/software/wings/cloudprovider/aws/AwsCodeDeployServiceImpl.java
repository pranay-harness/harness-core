package software.wings.cloudprovider.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.CreateDeploymentResult;
import com.amazonaws.services.codedeploy.model.ListApplicationsRequest;
import com.amazonaws.services.codedeploy.model.ListApplicationsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsResult;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.CodeDeployDeploymentInfo;
import software.wings.service.impl.AwsHelperService;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 6/22/17.
 */
@Singleton
public class AwsCodeDeployServiceImpl implements AwsCodeDeployService {
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
    // TODO:: integrate deployment and instance API
    // fetch deployment status.
    // block on status till success/failure
    // figure out instances deployed.
    // return instnces summary with status

    return new CodeDeployDeploymentInfo();
  }
}
