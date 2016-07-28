package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
  @ValidationGroups(Create.class) ServiceTemplate save(@Valid ServiceTemplate serviceTemplate);

  /**
   * Update.
   *
   * @param serviceTemplate the service template
   * @return the service template
   */
  @ValidationGroups(Update.class) ServiceTemplate update(@Valid ServiceTemplate serviceTemplate);

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
  void delete(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String serviceTemplateId);

  /**
   * Gets the.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceTemplateId the service template id
   * @return the service template
   */
  ServiceTemplate get(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String serviceTemplateId);

  /**
   * Update hosts.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceTemplateId the service template id
   * @param hostIds           the host ids
   * @return the service template
   */
  ServiceTemplate updateHosts(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String serviceTemplateId,
      @NotNull List<String> hostIds);

  /**
   * Update tags.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceTemplateId the service template id
   * @param tagIds            the tag ids
   * @return the service template
   */
  ServiceTemplate updateTags(
      @NotEmpty String appId, @NotEmpty String envId, @NotEmpty String serviceTemplateId, @NotNull List<String> tagIds);

  /**
   * Gets the tagged hosts.
   *
   * @param appId       the app id
   * @param envId       the env id
   * @param templateId  the template id
   * @param pageRequest the page request   @return the tagged hosts
   * @return the tagged hosts
   */
  PageResponse<Host> getTaggedHosts(
      @NotEmpty String appId, @NotEmpty String envId, @NotEmpty String templateId, PageRequest<Host> pageRequest);

  /**
   * Delete host from templates.
   *
   * @param host the host
   */
  void deleteHostFromTemplates(@NotNull Host host);

  /**
   * Gets templates by leaf tag.
   *
   * @param tag the tag
   * @return the templates by leaf tag
   */
  List<ServiceTemplate> getTemplatesByLeafTag(@NotNull Tag tag);

  /**
   * Gets template by mapped tags.
   *
   * @param tags the tags
   * @return the template by mapped tags
   */
  List<ServiceTemplate> getTemplateByMappedTags(@NotNull List<Tag> tags);

  /**
   * Add leaf tag.
   *
   * @param template the template
   * @param tag      the tag
   */
  void addLeafTag(ServiceTemplate template, Tag tag);

  /**
   * Delete by env.
   *
   * @param appId the app id
   * @param envId the env id
   */
  void deleteByEnv(@NotEmpty String appId, @NotEmpty String envId);

  /**
   * Delete by service.
   *
   * @param appId     the app id
   * @param serviceId the service id
   */
  void deleteByService(@NotEmpty String appId, @NotEmpty String serviceId);

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
   * Add hosts.
   *
   * @param template the template
   * @param hosts    the hosts
   * @return the service template
   */
  ServiceTemplate addHosts(ServiceTemplate template, List<Host> hosts);

  List<Key<ServiceTemplate>> getTemplateRefKeysByService(String appId, String envId, String serviceId);
}
