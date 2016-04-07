package software.wings.service.impl;

import com.google.inject.Inject;
import software.wings.beans.Host;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceTemplateService;

import javax.inject.Singleton;

/**
 * Created by anubhaw on 4/4/16.
 */

@Singleton
public class ServiceTemplateServiceImpl implements ServiceTemplateService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<ServiceTemplate> list(String envID, PageRequest<ServiceTemplate> pageRequest) {
    return wingsPersistence.query(ServiceTemplate.class, pageRequest);
  }

  @Override
  public ServiceTemplate createServiceTemplate(String envID, ServiceTemplate serviceTemplate) {
    serviceTemplate.setEnvID(envID);
    return wingsPersistence.saveAndGet(ServiceTemplate.class, serviceTemplate);
  }

  @Override
  public ServiceTemplate updateServiceTemplate(String envID, ServiceTemplate serviceTemplate) {
    return wingsPersistence.saveAndGet(ServiceTemplate.class, serviceTemplate);
  }
}
