package software.wings.service.impl;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.sshd.common.util.GenericUtils.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Command.Builder.aCommand;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.ErrorConstants.DUPLICATE_COMMAND_NAMES;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import com.mongodb.BasicDBObject;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Application;
import software.wings.beans.Command;
import software.wings.beans.CommandUnitType;
import software.wings.beans.Graph;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Validator;

import java.util.List;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 3/25/16.
 */
@ValidateOnExecution
public class ServiceResourceServiceImpl implements ServiceResourceService {
  private WingsPersistence wingsPersistence;
  private ConfigService configService;

  @Inject
  public ServiceResourceServiceImpl(WingsPersistence wingsPersistence, ConfigService configService) {
    this.wingsPersistence = wingsPersistence;
    this.configService = configService;
  }

  @Override
  public PageResponse<Service> list(PageRequest<Service> request) {
    return wingsPersistence.query(Service.class, request);
  }

  @Override
  public Service save(Service service) {
    Service savedService = wingsPersistence.saveAndGet(Service.class, service);
    wingsPersistence.addToList(Application.class, service.getAppId(), "services", savedService); // TODO: remove it
    return savedService;
  }

  @Override
  public Service update(Service service) {
    wingsPersistence.updateFields(Service.class, service.getUuid(),
        ImmutableMap.of("name", service.getName(), "description", service.getDescription(), "artifactType",
            service.getArtifactType(), "appContainer", service.getAppContainer()));
    return wingsPersistence.get(Service.class, service.getAppId(), service.getUuid());
  }

  @Override
  public Service get(String appId, String serviceId) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    if (service != null) {
      service.setConfigFiles(configService.getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, service.getUuid()));
    }
    return service;
  }

  @Override
  public void delete(String appId, String serviceId) {
    wingsPersistence.delete(Service.class, serviceId);
  }

  @Override
  public Service addCommand(String appId, String serviceId, Graph commandGraph) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    Validator.notNullCheck("service", service);

    if (!commandGraph.isLinear()) {
      throw new IllegalArgumentException("Graph is not a pipeline");
    }

    Command command = aCommand().withGraph(commandGraph).build();
    command.transformGraph();
    command.setServiceId(serviceId);

    if (!wingsPersistence.addToList(Service.class, appId, serviceId,
            wingsPersistence.createQuery(Service.class).field("commands.name").notEqual(command.getName()), "commands",
            command)) {
      throw new WingsException(DUPLICATE_COMMAND_NAMES, "commandName", command.getName());
    }

    return get(appId, serviceId);
  }

  @Override
  public Service deleteCommand(String appId, String serviceId, String commandName) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    Validator.notNullCheck("service", service);

    wingsPersistence.update(
        wingsPersistence.createQuery(Service.class).field(ID_KEY).equal(appId).field("appId").equal(appId),
        wingsPersistence.createUpdateOperations(Service.class)
            .removeAll("commands", new BasicDBObject("name", commandName)));

    return get(appId, serviceId);
  }

  @Override
  public List<Object> getCommandStencils(@NotEmpty String appId, @NotEmpty String serviceId) {
    Service service = get(appId, serviceId);
    if (isEmpty(service.getCommands())) {
      return emptyList();
    } else {
      return service.getCommands()
          .stream()
          .map(command -> Maps.newHashMap(ImmutableMap.of("name", command.getName(), "type", CommandUnitType.COMMAND)))
          .collect(toList());
    }
  }
}
