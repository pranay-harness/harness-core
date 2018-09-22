package software.wings.service.intfc.yaml;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.ConfigFile;
import software.wings.beans.RestResponse;
import software.wings.yaml.YamlPayload;

/**
 * Yaml Resource Service.
 *
 * @author bsollish
 */
public interface YamlResourceService {
  /**
   * Find by app, service and service command ids.
   *
   * @param appId     the app id
   * @param serviceCommandId the service command id
   * @return the service command
   */
  RestResponse<YamlPayload> getServiceCommand(@NotEmpty String appId, @NotEmpty String serviceCommandId);

  /**
   * Gets the yaml version of a pipeline by pipelineId
   *
   * @param appId     the app id
   * @param pipelineId the pipeline id
   * @return the rest response
   */
  RestResponse<YamlPayload> getPipeline(String appId, String pipelineId);

  /**
   * Gets the yaml version of a trigger by artifactStreamId
   *
   * @param appId     the app id
   * @param artifactStreamId the artifact stream id
   * @return the rest response
   */
  RestResponse<YamlPayload> getTrigger(String appId, String artifactStreamId);

  /**
   * Gets the yaml for a workflow
   *
   * @param appId     the app id
   * @param workflowId the workflow id
   * @return the rest response
   */
  RestResponse<YamlPayload> getWorkflow(String appId, String workflowId);

  /**
   * Gets all the setting attributes of a given type by accountId
   *
   * @param accountId   the account id
   * @param type        the SettingVariableTypes
   * @return the rest response
   */
  RestResponse<YamlPayload> getGlobalSettingAttributesList(String accountId, String type);

  /**
   * Gets the yaml version of an environment by envId
   *
   * @param appId   the app id
   * @param envId   the environment id
   * @return the rest response
   */
  RestResponse<YamlPayload> getEnvironment(String appId, String envId);

  RestResponse<YamlPayload> getService(String appId, String serviceId);

  RestResponse<YamlPayload> getInfraMapping(String accountId, String appId, String infraMappingId);

  RestResponse<YamlPayload> getContainerTask(String accountId, String appId, String containerTaskId);

  RestResponse<YamlPayload> getHelmChartSpecification(String accountId, String appId, String helmChartSpecificationId);

  RestResponse<YamlPayload> getLambdaSpec(String accountId, String appId, String lambdaSpecId);

  RestResponse<YamlPayload> getUserDataSpec(String accountId, String appId, String userDataSpecId);

  RestResponse<YamlPayload> getSettingAttribute(String accountId, String uuid);

  /**
   * Gets all the default variables of a given type by accountId and appId
   * @param accountId   the account id
   * @param appId   the app id
   * @return the rest responsegetDefaultVariableList
   */
  RestResponse<YamlPayload> getDefaultVariables(String accountId, String appId);

  /**
   * Get config file yaml
   * @param appId
   * @param configFileUuid
   * @return
   */
  RestResponse<YamlPayload> getConfigFileYaml(String accountId, String appId, String configFileUuid);

  RestResponse<YamlPayload> getConfigFileYaml(String accountId, String appId, ConfigFile configFile);

  RestResponse<YamlPayload> getNotificationGroup(String accountId, String notificationGroupUuid);

  RestResponse<YamlPayload> getPcfServiceSpecification(
      String accountId, String appId, String pcfServiceSpecificationId);

  RestResponse<YamlPayload> getProvisioner(String appId, String provisionerId);

  <T> RestResponse<YamlPayload> obtainEntityYamlVersion(String accountId, T entity);
}
