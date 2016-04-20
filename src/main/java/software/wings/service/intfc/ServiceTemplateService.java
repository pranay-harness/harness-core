package software.wings.service.intfc;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.ServiceTemplate;

/**
 * Created by anubhaw on 4/4/16.
 */
public interface ServiceTemplateService {
  PageResponse<ServiceTemplate> list(String envID, PageRequest<ServiceTemplate> pageRequest);

  ServiceTemplate createServiceTemplate(String envID, ServiceTemplate serviceTemplate);

  ServiceTemplate updateServiceTemplate(String envID, ServiceTemplate serviceTemplate);
}
