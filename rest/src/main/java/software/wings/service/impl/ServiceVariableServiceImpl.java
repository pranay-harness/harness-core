package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.EntityType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.utils.Validator;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
@Singleton
public class ServiceVariableServiceImpl implements ServiceVariableService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceTemplateService serviceTemplateService;

  @Override
  public PageResponse<ServiceVariable> list(PageRequest<ServiceVariable> request) {
    PageResponse<ServiceVariable> response = wingsPersistence.query(ServiceVariable.class, request);
    response.getResponse().forEach(serviceVariable -> maskEncryptedFields(serviceVariable));
    return response;
  }

  @Override
  public ServiceVariable save(@Valid ServiceVariable serviceVariable) {
    if (!Arrays.asList(SERVICE, EntityType.SERVICE_TEMPLATE, EntityType.SERVICE_TEMPLATE, EntityType.HOST)
             .contains(serviceVariable.getEntityType())) {
      throw new WingsException(
          INVALID_ARGUMENT, "args", "Service setting not supported for entityType " + serviceVariable.getEntityType());
    }
    String envId = serviceVariable.getEntityType().equals(SERVICE)
        ? GLOBAL_ENV_ID
        : serviceTemplateService.get(serviceVariable.getAppId(), serviceVariable.getTemplateId()).getEnvId();

    serviceVariable.setEnvId(envId);
    return Validator.duplicateCheck(
        () -> wingsPersistence.saveAndGet(ServiceVariable.class, serviceVariable), "name", serviceVariable.getName());
  }

  @Override
  public ServiceVariable get(@NotEmpty String appId, @NotEmpty String settingId) {
    ServiceVariable serviceVariable = wingsPersistence.get(ServiceVariable.class, appId, settingId);
    Validator.notNullCheck("ServiceVariable", serviceVariable);
    maskEncryptedFields(serviceVariable);
    return serviceVariable;
  }

  @Override
  public ServiceVariable update(@Valid ServiceVariable serviceVariable) {
    ServiceVariable savedServiceVariable = get(serviceVariable.getAppId(), serviceVariable.getUuid());
    Validator.notNullCheck("Service variable", savedServiceVariable);

    if (savedServiceVariable.getEntityType().equals(SERVICE)
        && !savedServiceVariable.getName().equals(serviceVariable.getName())) {
      updateVariableNameForServiceAndOverrides(savedServiceVariable, serviceVariable.getName());
    }

    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(),
        ImmutableMap.of("value", serviceVariable.getValue(), "type", serviceVariable.getType()));

    return get(serviceVariable.getAppId(), serviceVariable.getUuid());
  }

  private void updateVariableNameForServiceAndOverrides(ServiceVariable existingServiceVariable, String name) {
    List<Object> templateIds = serviceTemplateService
                                   .getTemplateRefKeysByService(
                                       existingServiceVariable.getAppId(), existingServiceVariable.getEntityId(), null)
                                   .stream()
                                   .map(Key::getId)
                                   .collect(Collectors.toList());

    Query<ServiceVariable> query =
        wingsPersistence.createQuery(ServiceVariable.class).field("appId").equal(existingServiceVariable.getAppId());
    query.or(query.criteria("entityId").equal(existingServiceVariable.getEntityId()),
        query.criteria("templateId").in(templateIds));

    UpdateOperations<ServiceVariable> updateOperations =
        wingsPersistence.createUpdateOperations(ServiceVariable.class).set("name", name);
    wingsPersistence.update(query, updateOperations);
  }

  @Override
  public void delete(@NotEmpty String appId, @NotEmpty String settingId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(ServiceVariable.class).field("appId").equal(appId).field(ID_KEY).equal(settingId));
  }

  @Override
  public List<ServiceVariable> getServiceVariablesForEntity(String appId, String templateId, String entityId) {
    List<ServiceVariable> variables =
        list(aPageRequest()
                 .addFilter(aSearchFilter().withField("appId", Operator.EQ, appId).build())
                 .addFilter(aSearchFilter().withField("templateId", Operator.EQ, templateId).build())
                 .addFilter(aSearchFilter().withField("entityId", Operator.EQ, entityId).build())
                 .build())
            .getResponse();
    variables.forEach(serviceVariable -> maskEncryptedFields(serviceVariable));
    return variables;
  }

  @Override
  public List<ServiceVariable> getServiceVariablesByTemplate(
      String appId, String envId, ServiceTemplate serviceTemplate) {
    List<ServiceVariable> variables = wingsPersistence.createQuery(ServiceVariable.class)
                                          .field("appId")
                                          .equal(appId)
                                          .field("envId")
                                          .equal(envId)
                                          .field("templateId")
                                          .equal(serviceTemplate.getUuid())
                                          .asList();
    variables.forEach(serviceVariable -> maskEncryptedFields(serviceVariable));
    return variables;
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

  private void maskEncryptedFields(ServiceVariable serviceVariable) {
    if (serviceVariable.getType() == Type.ENCRYPTED_TEXT) {
      serviceVariable.setValue("******".toCharArray());
    }
  }
}
