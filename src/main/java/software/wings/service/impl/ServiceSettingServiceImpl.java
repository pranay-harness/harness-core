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
import software.wings.beans.ServiceSetting;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ServiceSettingService;
import software.wings.service.intfc.ServiceTemplateService;

import java.util.Arrays;
import java.util.List;
import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
public class ServiceSettingServiceImpl implements ServiceSettingService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceTemplateService serviceTemplateService;

  @Override
  public PageResponse<ServiceSetting> list(PageRequest<ServiceSetting> request) {
    return wingsPersistence.query(ServiceSetting.class, request);
  }

  @Override
  public String save(@Valid ServiceSetting serviceSetting) {
    if (!Arrays.asList(SERVICE, EntityType.TAG, EntityType.HOST).contains(serviceSetting.getEntityType())) {
      throw new WingsException(
          INVALID_ARGUMENT, "args", "Service setting not supported for entityType " + serviceSetting.getEntityType());
    }
    String envId = serviceSetting.getEntityType().equals(SERVICE)
        ? GLOBAL_ENV_ID
        : serviceTemplateService.get(serviceSetting.getAppId(), serviceSetting.getTemplateId()).getEnvId();

    serviceSetting.setEnvId(envId);
    return wingsPersistence.save(serviceSetting);
  }

  @Override
  public ServiceSetting get(@NotEmpty String appId, @NotEmpty String settingId) {
    ServiceSetting serviceSetting = wingsPersistence.get(ServiceSetting.class, appId, settingId);
    if (serviceSetting == null) {
      throw new WingsException(INVALID_ARGUMENT, "message", "Service Setting not found");
    }

    return serviceSetting;
  }

  @Override
  public ServiceSetting update(@Valid ServiceSetting serviceSetting) {
    return wingsPersistence.saveAndGet(ServiceSetting.class, serviceSetting);
  }

  @Override
  public void delete(@NotEmpty String appId, @NotEmpty String settingId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(ServiceSetting.class).field("appId").equal(appId).field(ID_KEY).equal(settingId));
  }

  @Override
  public List<ServiceSetting> getSettingsForEntity(String appId, String templateId, String entityId) {
    return list(aPageRequest()
                    .addFilter(aSearchFilter().withField("appId", Operator.EQ, appId).build())
                    .addFilter(aSearchFilter().withField("templateId", Operator.EQ, templateId).build())
                    .addFilter(aSearchFilter().withField("entityId", Operator.EQ, entityId).build())
                    .build())
        .getResponse();
  }

  @Override
  public List<ServiceSetting> getServiceSettingByTemplate(String appId, String envId, ServiceTemplate serviceTemplate) {
    return wingsPersistence.createQuery(ServiceSetting.class)
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
    wingsPersistence.delete(wingsPersistence.createQuery(ServiceSetting.class)
                                .field("appId")
                                .equal(appId)
                                .field("templateId")
                                .equal(templateId)
                                .field("entityId")
                                .equal(entityId));
  }

  @Override
  public void deleteByTemplateId(String appId, String serviceTemplateId) {
    wingsPersistence.delete(wingsPersistence.createQuery(ServiceSetting.class)
                                .field("appId")
                                .equal(appId)
                                .field("templateId")
                                .equal(serviceTemplateId));
  }

  @Override
  public void deleteByEntityId(String appId, String entityId) {
    wingsPersistence.delete(wingsPersistence.createQuery(ServiceSetting.class)
                                .field("appId")
                                .equal(appId)
                                .field("entityId")
                                .equal(entityId));
  }
}
