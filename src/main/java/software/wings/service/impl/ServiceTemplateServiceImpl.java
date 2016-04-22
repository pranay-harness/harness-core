package software.wings.service.impl;

import com.google.inject.Inject;

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
  public PageResponse<ServiceTemplate> list(String envId, PageRequest<ServiceTemplate> pageRequest) {
    return wingsPersistence.query(ServiceTemplate.class, pageRequest);
  }

  @Override
  public ServiceTemplate createServiceTemplate(String envId, ServiceTemplate serviceTemplate) {
    serviceTemplate.setEnvId(envId);
    return wingsPersistence.saveAndGet(ServiceTemplate.class, serviceTemplate);
  }

  @Override
  public ServiceTemplate updateServiceTemplate(String envId, ServiceTemplate serviceTemplate) {
    return wingsPersistence.saveAndGet(ServiceTemplate.class, serviceTemplate);
  }
}
