package software.wings.cloudprovider.aws;

import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.RevisionLocation;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.CodeDeployDeploymentInfo;

import java.util.List;

/**
 * Created by anubhaw on 6/22/17.
 */
public interface AwsCodeDeployService {
  /**
   * List applications list.
   *
   * @param region               the region
   * @param cloudProviderSetting the cloud provider setting
   * @return the list
   */
  List<String> listApplications(String region, SettingAttribute cloudProviderSetting);

  /**
   * List deployment group list.
   *
   * @param region               the region
   * @param appName              the app name
   * @param cloudProviderSetting the cloud provider setting
   * @return the list
   */
  List<String> listDeploymentGroup(String region, String appName, SettingAttribute cloudProviderSetting);

  /**
   * List deployment configuration list.
   *
   * @param region               the region
   * @param cloudProviderSetting the cloud provider setting
   * @return the list
   */
  List<String> listDeploymentConfiguration(String region, SettingAttribute cloudProviderSetting);

  /**
   * Deploy application.
   *
   * @param region                  the region
   * @param cloudProviderSetting    the cloud provider setting
   * @param createDeploymentRequest the create deployment request
   * @param executionLogCallback    the execution log callback
   * @return the code deploy deployment info
   */
  CodeDeployDeploymentInfo deployApplication(String region, SettingAttribute cloudProviderSetting,
      CreateDeploymentRequest createDeploymentRequest, ExecutionLogCallback executionLogCallback);

  /**
   * Gets application current revision info.
   *
   * @param region               the region
   * @param appName              the app name
   * @param revisionType         the revision type
   * @param cloudProviderSetting the cloud provider setting
   * @return the application current revision info
   */
  List<RevisionLocation> getApplicationRevisionList(
      String region, String appName, String revisionType, SettingAttribute cloudProviderSetting);
}
