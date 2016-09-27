package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Inject;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EntityType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;

import java.util.Arrays;
import java.util.List;
import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
public class ServiceVariableServiceImpl implements ServiceVariableService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceTemplateService serviceTemplateService;

  @Override
  public PageResponse<ServiceVariable> list(PageRequest<ServiceVariable> request) {
    return wingsPersistence.query(ServiceVariable.class, request);
  }

  @Override
  public ServiceVariable save(@Valid ServiceVariable serviceVariable) {
    if (!Arrays.asList(SERVICE, EntityType.TAG, EntityType.HOST).contains(serviceVariable.getEntityType())) {
      throw new WingsException(
          INVALID_ARGUMENT, "args", "Service setting not supported for entityType " + serviceVariable.getEntityType());
    }
    String envId = serviceVariable.getEntityType().equals(SERVICE)
        ? GLOBAL_ENV_ID
        : serviceTemplateService.get(serviceVariable.getAppId(), serviceVariable.getTemplateId()).getEnvId();

    serviceVariable.setEnvId(envId);
    return wingsPersistence.saveAndGet(ServiceVariable.class, serviceVariable);
  }

  @Override
  public ServiceVariable get(@NotEmpty String appId, @NotEmpty String settingId) {
    ServiceVariable serviceVariable = wingsPersistence.get(ServiceVariable.class, appId, settingId);
    if (serviceVariable == null) {
      throw new WingsException(INVALID_ARGUMENT, "message", "Service Setting not found");
    }

    return serviceVariable;
  }

  @Override
  public ServiceVariable update(@Valid ServiceVariable serviceVariable) {
    return wingsPersistence.saveAndGet(ServiceVariable.class, serviceVariable);
  }

  @Override
  public void delete(@NotEmpty String appId, @NotEmpty String settingId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(ServiceVariable.class).field("appId").equal(appId).field(ID_KEY).equal(settingId));
  }

  @Override
  public List<ServiceVariable> getServiceVariablesForEntity(String appId, String templateId, String entityId) {
    return list(aPageRequest()
                    .addFilter(aSearchFilter().withField("appId", Operator.EQ, appId).build())
                    .addFilter(aSearchFilter().withField("templateId", Operator.EQ, templateId).build())
                    .addFilter(aSearchFilter().withField("entityId", Operator.EQ, entityId).build())
                    .build())
        .getResponse();
  }

  @Override
  public List<ServiceVariable> getServiceVariablesByTemplate(
      String appId, String envId, ServiceTemplate serviceTemplate) {
    return wingsPersistence.createQuery(ServiceVariable.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field("templateId")
        .equal(serviceTemplate.getUuid())
        .asList();
  }

  @Override
  public void deleteByEntityId(String appId, String templateId, String entityId) {
    wingsPersistence.delete(wingsPersistence.createQuery(ServiceVariable.class)
                                .field("appId")
                                .equal(appId)
                                .field("templateId")
                                .equal(templateId)
                                .field("entityId")
                                .equal(entityId));
  }

  @Override
  public void deleteByTemplateId(String appId, String serviceTemplateId) {
    wingsPersistence.delete(wingsPersistence.createQuery(ServiceVariable.class)
                                .field("appId")
                                .equal(appId)
                                .field("templateId")
                                .equal(serviceTemplateId));
  }

  @Override
  public void deleteByEntityId(String appId, String entityId) {
    wingsPersistence.delete(wingsPersistence.createQuery(ServiceVariable.class)
                                .field("appId")
                                .equal(appId)
                                .field("entityId")
                                .equal(entityId));
  }
}
