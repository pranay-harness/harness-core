package software.wings.service.intfc;

import software.wings.beans.ConfigFile;
import software.wings.beans.Host;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/4/16.
 */
public interface ServiceTemplateService {
  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<ServiceTemplate> list(PageRequest<ServiceTemplate> pageRequest);

  /**
   * Save.
   *
   * @param serviceTemplate the service template
   * @return the service template
   */
  ServiceTemplate save(ServiceTemplate serviceTemplate);

  /**
   * Update.
   *
   * @param serviceTemplate the service template
   * @return the service template
   */
  ServiceTemplate update(ServiceTemplate serviceTemplate);

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
  Map<String, List<ConfigFile>> computedConfigFiles(String appId, String envId, String templateId);

  /**
   * Delete.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceTemplateId the service template id
   */
  void delete(String appId, String envId, String serviceTemplateId);

  /**
   * Gets the.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceTemplateId the service template id
   * @return the service template
   */
  ServiceTemplate get(String appId, String envId, String serviceTemplateId);

  /**
   * Update hosts.
   *
   * @param appId             the app id
   * @param serviceTemplateId the service template id
   * @param hostIds           the host ids
   * @return the service template
   */
  ServiceTemplate updateHosts(
      @NotNull String appId, @NotNull String envId, @NotNull String serviceTemplateId, @NotNull List<String> hostIds);

  /**
   * Update tags.
   *
   * @param appId             the app id
   * @param serviceTemplateId the service template id
   * @param tagIds            the tag ids
   * @return the service template
   */
  ServiceTemplate updateTags(
      @NotNull String appId, @NotNull String envId, @NotNull String serviceTemplateId, @NotNull List<String> tagIds);

  /**
   * Gets the tagged hosts.
   *
   * @param templateId  the template id
   * @param pageRequest the page request
   * @return the tagged hosts
   */
  PageResponse<Host> getTaggedHosts(String templateId, PageRequest<Host> pageRequest);

  void deleteHostFromTemplates(Host host);

  List<ServiceTemplate> getTemplatesByTag(Tag tag);
}
