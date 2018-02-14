package software.wings.cloudprovider.aws;

import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.RevisionLocation;
import com.amazonaws.services.ec2.model.Instance;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.CodeDeployDeploymentInfo;
import software.wings.security.encryption.EncryptedDataDetail;

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
   * @param encryptionDetails
   * @return the list
   */
  List<String> listApplications(
      String region, SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptionDetails);

  /**
   * List deployment group list.
   *
   * @param region               the region
   * @param appName              the app name
   * @param cloudProviderSetting the cloud provider setting
   * @param encryptedDataDetails
   * @return the list
   */
  List<String> listDeploymentGroup(String region, String appName, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * List deployment configuration list.
   *
   * @param region               the region
   * @param cloudProviderSetting the cloud provider setting
   * @param encryptedDataDetails
   * @return the list
   */
  List<String> listDeploymentConfiguration(
      String region, SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * Deploy application.
   *
   * @param region                  the region
   * @param cloudProviderSetting    the cloud provider setting
   * @param encryptedDataDetails
   *@param createDeploymentRequest the create deployment request
   * @param executionLogCallback    the execution log callback   @return the code deploy deployment info
   */
  CodeDeployDeploymentInfo deployApplication(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, CreateDeploymentRequest createDeploymentRequest,
      ExecutionLogCallback executionLogCallback);

  /**
   * Get all the instances from the given codeDeploy deployment group
   * @param region                aws region
   * @param cloudProviderSetting  cloud provider setting
   * @param encryptedDataDetails  encrypted data details
   * @param deploymentId          deployment id
   * @return
   */
  List<Instance> listDeploymentInstances(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String deploymentId);

  /**
   * Gets application current revision info.
   *
   * @param region               the region
   * @param appName              the app name
   * @param cloudProviderSetting the cloud provider setting
   * @param encryptedDataDetails
   * @return the application current revision info
   */
  RevisionLocation getApplicationRevisionList(String region, String appName, String deploymentGroupName,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails);
}
