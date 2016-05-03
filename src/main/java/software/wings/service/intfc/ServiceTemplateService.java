package software.wings.service.intfc;

import software.wings.beans.ConfigFile;
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

  ServiceTemplate updateHostAndTags(String serviceTemplateId, List<String> tagIds, List<String> hostIds);

  List<ConfigFile> overrideConfigFiles(List<ConfigFile> existingFiles, List<ConfigFile> newFiles);

  Map<String, List<ConfigFile>> computedConfigFiles(String templateId);
}
