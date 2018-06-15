package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.container.KubernetesPayload;
import software.wings.beans.stats.CloneMetadata;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByApplication;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 4/1/16.
 */
public interface EnvironmentService extends OwnedByApplication {
  /**
   * List.
   *
   * @param request     the request
   * @param withSummary the with summary
   * @return the page response
   */
  PageResponse<Environment> list(PageRequest<Environment> request, boolean withSummary);

  /**
   * Gets the.
   *
   * @param appId       the app id
   * @param envId       the env id
   * @param withSummary the with summary
   * @return the environment
   */
  Environment get(@NotEmpty String appId, @NotEmpty String envId, boolean withSummary);

  /**
   * Gets the environment and does not throw exception if not found
   * @param appId
   * @param envId
   * @return
   */
  Environment get(@NotEmpty String appId, @NotEmpty String envId);

  /**
   * Get environment.
   *
   * @param appId  the app id
   * @param envId  the env id
   * @param status the status
   * @return the environment
   */
  Environment get(@NotEmpty String appId, @NotEmpty String envId, @NotNull SetupStatus status);

  Environment getEnvironmentByName(String appId, String environmentName);

  Environment getEnvironmentByName(String appId, String environmentName, boolean withServiceTemplates);

  /**
   * Exist boolean.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the boolean
   */
  boolean exist(@NotEmpty String appId, @NotEmpty String envId);

  /**
   * Save.
   *
   * @param environment the environment
   * @return the environment
   */
  @ValidationGroups(Create.class) Environment save(@Valid Environment environment);

  /**
   * Update.
   *
   * @param environment the environment
   * @return the environment
   */
  @ValidationGroups(Update.class) Environment update(@Valid Environment environment);

  /**
   * Delete.
   *
   * @param appId the app id
   * @param envId the env id
   */
  void delete(@NotEmpty String appId, @NotEmpty String envId);

  /**
   * Prune descending objects.
   *
   * @param appId the app id
   * @param envId the env id
   */
  void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String envId);

  /**
   * Create default environments.
   *
   * @param appId the app id
   */
  void createDefaultEnvironments(@NotEmpty String appId);

  /**
   * Gets env by app.
   *
   * @param appId the app id
   * @return the env by app
   */
  List<Environment> getEnvByApp(@NotEmpty String appId);

  /**
   * Gets env by app.
   *
   * @param appIds the app id
   * @return the env by app
   */
  List<Environment> findEnvNamesByAppIds(@NotEmpty List<String> appIds);

  /**
   * Clones Environment along with Service Infrastructure
   *
   * @param appId
   * @param envId
   * @param cloneMetadata
   * @return
   */
  Environment cloneEnvironment(@NotEmpty String appId, @NotEmpty String envId, CloneMetadata cloneMetadata);

  /**
   * @param appId
   * @param envId
   * @return
   */
  List<Service> getServicesWithOverrides(@NotEmpty String appId, @NotEmpty String envId);

  Environment setConfigMapYaml(String appId, String envId, KubernetesPayload kubernetesPayload);

  Environment setConfigMapYamlForService(
      String appId, String envId, String serviceTemplateId, KubernetesPayload kubernetesPayload);

  Environment setHelmValueYaml(String appId, String envId, KubernetesPayload kubernetesPayload);

  Environment setHelmValueYamlForService(
      String appId, String envId, String serviceTemplateId, KubernetesPayload kubernetesPayload);
}
