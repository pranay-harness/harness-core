package software.wings.service.intfc;

import software.wings.beans.ConfigFile;
import software.wings.beans.Host;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.ServiceTemplate;

import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 4/4/16.
 */
public interface ServiceTemplateService {
  PageResponse<ServiceTemplate> list(PageRequest<ServiceTemplate> pageRequest);

  ServiceTemplate save(ServiceTemplate serviceTemplate);

  ServiceTemplate update(ServiceTemplate serviceTemplate);

  List<ConfigFile> overrideConfigFiles(List<ConfigFile> existingFiles, List<ConfigFile> newFiles);

  Map<String, List<ConfigFile>> computedConfigFiles(String appId, String envId, String templateId);

  void delete(String appId, String envId, String serviceTemplateId);

  ServiceTemplate get(String appId, String envId, String serviceTemplateId);

  ServiceTemplate updateHosts(String appId, String serviceTemplateId, List<String> hostIds);

  ServiceTemplate updateTags(String appId, String serviceTemplateId, List<String> tagIds);

  PageResponse<Host> getTaggedHosts(String templateId, PageRequest<Host> pageRequest);
}
