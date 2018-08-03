package software.wings.service.intfc;

import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByEnvironment;
import software.wings.service.intfc.ownership.OwnedByService;

import java.util.List;
import javax.validation.Valid;

/**
 * Created by anubhaw on 4/4/16.
 */
public interface ServiceTemplateService extends OwnedByEnvironment, OwnedByService {
  /**
   * List.
   *
   * @param pageRequest the page request
   * @param withDetails the with details
   * @return the page response
   */
  PageResponse<ServiceTemplate> list(
      PageRequest<ServiceTemplate> pageRequest, boolean withDetails, boolean maskEncryptedFields);

  /**
   * Save.
   *
   * @param serviceTemplate the service template
   * @return the service template
   */
  @ValidationGroups(Create.class) ServiceTemplate save(@Valid ServiceTemplate serviceTemplate);

  /**
   * Update.
   *
   * @param serviceTemplate the service template
   * @return the service template
   */
  @ValidationGroups(Update.class) ServiceTemplate update(@Valid ServiceTemplate serviceTemplate);

  /**
   * Compute service variables map.
   *
   * @param appId      the app id
   * @param envId      the env id
   * @param templateId the template id
   * @return the map
   */
  List<ServiceVariable> computeServiceVariables(
      String appId, String envId, String templateId, String workflowExecutionId, boolean maskEncryptedFields);

  String computeConfigMapYaml(String appId, String envId, String templateId);

  List<String> helmValueOverridesYamlFiles(String appId, String templateId);

  /**
   * Override config files.
   *
   * @param existingFiles the existing files
   * @param newFiles      the new files
   * @return the list
   */
  List<ConfigFile> overrideConfigFiles(List<ConfigFile> existingFiles, List<ConfigFile> newFiles);

  /**
   * Computed config files.
   *
   * @param appId      the app id
   * @param envId      the env id
   * @param templateId the template id
   * @return the map
   */
  List<ConfigFile> computedConfigFiles(String appId, String envId, String templateId);

  /**
   * Delete.
   *
   * @param appId             the app id
   * @param serviceTemplateId the service template id
   */
  void delete(@NotEmpty String appId, @NotEmpty String serviceTemplateId);

  /**
   * Gets the.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceTemplateId the service template id
   * @param withDetails       the with details
   * @param maskEncryptedFields the mask encryted fields
   * @return the service template
   */
  ServiceTemplate get(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String serviceTemplateId,
      boolean withDetails, boolean maskEncryptedFields);

  /**
   * Get service template.
   *
   * @param appId             the app id
   * @param serviceTemplateId the service template id
   * @return the service template
   */
  ServiceTemplate get(@NotEmpty String appId, @NotEmpty String serviceTemplateId);

  /**
   * Get service template.
   *
   * @param appId             the app id
   * @param serviceId         the service id
   * @param environmentId             the environment id
   * @return the service template
   */
  ServiceTemplate get(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String environmentId);

  /**
   * Create default templates by env.
   *
   * @param env the env
   */
  void createDefaultTemplatesByEnv(Environment env);

  /**
   * Create default templates by service.
   *
   * @param service the service
   */
  void createDefaultTemplatesByService(Service service);

  /**
   * Gets template ref keys by service.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param envId     the env id
   * @return the template ref keys by service
   */
  List<Key<ServiceTemplate>> getTemplateRefKeysByService(String appId, String serviceId, String envId);

  /**
   * Update default service template name.
   *
   * @param appId          the app id
   * @param serviceId      the service id
   * @param oldServiceName the old service name
   * @param newServiceName the new service name
   */
  void updateDefaultServiceTemplateName(String appId, String serviceId, String oldServiceName, String newServiceName);

  /**
   * Exist boolean.
   *
   * @param appId      the app id
   * @param templateId the template id
   * @return the boolean
   */
  boolean exist(String appId, String templateId);
}
