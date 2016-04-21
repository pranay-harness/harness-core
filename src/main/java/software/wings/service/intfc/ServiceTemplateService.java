package software.wings.service.intfc;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.ServiceTemplate;

/**
 * Created by anubhaw on 4/4/16.
 */
public interface ServiceTemplateService {
  PageResponse<ServiceTemplate> list(String envId, PageRequest<ServiceTemplate> pageRequest);

  ServiceTemplate createServiceTemplate(String envId, ServiceTemplate serviceTemplate);

  ServiceTemplate updateServiceTemplate(String envId, ServiceTemplate serviceTemplate);
}
