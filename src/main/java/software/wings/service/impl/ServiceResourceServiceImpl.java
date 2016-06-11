package software.wings.service.impl;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.sshd.common.util.GenericUtils.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Command.Builder.aCommand;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.ErrorCodes.DUPLICATE_COMMAND_NAMES;
import static software.wings.utils.DefaultCommands.getInstallCommandGraph;
import static software.wings.utils.DefaultCommands.getStartCommandGraph;
import static software.wings.utils.DefaultCommands.getStopCommandGraph;

import com.google.common.collect.ImmutableMap;

import com.mongodb.BasicDBObject;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Application;
import software.wings.beans.Command;
import software.wings.beans.Graph;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Validator;

import java.util.Map;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 3/25/16.
 */
@ValidateOnExecution
public class ServiceResourceServiceImpl implements ServiceResourceService {
  private WingsPersistence wingsPersistence;
  private ConfigService configService;

  /**
   * Instantiates a new service resource service impl.
   *
   * @param wingsPersistence the wings persistence
   * @param configService    the config service
   */
  @Inject
  public ServiceResourceServiceImpl(WingsPersistence wingsPersistence, ConfigService configService) {
    this.wingsPersistence = wingsPersistence;
    this.configService = configService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Service> list(PageRequest<Service> request) {
    return wingsPersistence.query(Service.class, request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service save(Service service) {
    Service savedService = wingsPersistence.saveAndGet(Service.class, service);
    addDefaultCommands(savedService);
    wingsPersistence.addToList(Application.class, service.getAppId(), "services", savedService);
    return savedService;
  }

  private void addDefaultCommands(Service service) {
    addCommand(service.getAppId(), service.getUuid(), getInstallCommandGraph());
    addCommand(service.getAppId(), service.getUuid(), getStopCommandGraph());
    addCommand(service.getAppId(), service.getUuid(), getStartCommandGraph());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service update(Service service) {
    wingsPersistence.updateFields(Service.class, service.getUuid(),
        ImmutableMap.of("name", service.getName(), "description", service.getDescription(), "artifactType",
            service.getArtifactType(), "appContainer", service.getAppContainer()));
    return wingsPersistence.get(Service.class, service.getAppId(), service.getUuid());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service get(String appId, String serviceId) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    if (service != null) {
      service.setConfigFiles(configService.getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, service.getUuid()));
    }
    return service;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String appId, String serviceId) {
    wingsPersistence.delete(Service.class, serviceId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service addCommand(String appId, String serviceId, Graph commandGraph) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    Validator.notNullCheck("service", service);

    if (!commandGraph.isLinear()) {
      throw new IllegalArgumentException("Graph is not a pipeline");
    }

    Command command = aCommand().withGraph(commandGraph).build();
    command.transformGraph();

    if (!wingsPersistence.addToList(Service.class, appId, serviceId,
            wingsPersistence.createQuery(Service.class).field("commands.name").notEqual(command.getName()), "commands",
            command)) {
      throw new WingsException(DUPLICATE_COMMAND_NAMES, "commandName", command.getName());
    }

    return get(appId, serviceId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service deleteCommand(String appId, String serviceId, String commandName) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    Validator.notNullCheck("service", service);

    wingsPersistence.update(
        wingsPersistence.createQuery(Service.class).field(ID_KEY).equal(serviceId).field("appId").equal(appId),
        wingsPersistence.createUpdateOperations(Service.class)
            .removeAll("commands", new BasicDBObject("name", commandName)));

    return get(appId, serviceId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Command getCommandByName(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String commandName) {
    Service service = get(appId, serviceId);
    return service.getCommands()
        .stream()
        .filter(command -> equalsIgnoreCase(commandName, command.getName()))
        .findFirst()
        .orElse(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> getCommandStencils(@NotEmpty String appId, @NotEmpty String serviceId) {
    Service service = get(appId, serviceId);
    if (isEmpty(service.getCommands())) {
      return emptyMap();
    } else {
      return service.getCommands().stream().collect(toMap(Command::getName, Command::getName));
    }
  }
}
