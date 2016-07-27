package software.wings.service.impl;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.sshd.common.util.GenericUtils.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.ErrorCodes.DUPLICATE_COMMAND_NAMES;
import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.common.NotificationMessageResolver.ENTITY_CREATE_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.ENTITY_DELETE_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;
import static software.wings.utils.DefaultCommands.getInstallCommandGraph;
import static software.wings.utils.DefaultCommands.getStartCommandGraph;
import static software.wings.utils.DefaultCommands.getStopCommandGraph;

import com.google.common.collect.ImmutableMap;

import com.mongodb.BasicDBObject;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.Graph;
import software.wings.beans.Service;
import software.wings.beans.Setup.SetupStatus;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SetupService;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.Validator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 3/25/16.
 */
@ValidateOnExecution
@Singleton
public class ServiceResourceServiceImpl implements ServiceResourceService, DataProvider {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ConfigService configService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ExecutorService executorService;
  @Inject private StencilPostProcessor stencilPostProcessor;
  @Inject private AppService appService;
  @Inject private ActivityService activityService;
  @Inject private SetupService setupService;
  @Inject private NotificationService notificationService;

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Service> list(PageRequest<Service> request) {
    PageResponse<Service> pageResponse = wingsPersistence.query(Service.class, request);
    pageResponse.getResponse().forEach(service -> {
      service.setConfigFiles(
          configService.getConfigFilesForEntity(service.getAppId(), DEFAULT_TEMPLATE_ID, service.getUuid()));
    });
    return pageResponse;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service save(Service service) {
    Service savedService = wingsPersistence.saveAndGet(Service.class, service);
    savedService = addDefaultCommands(savedService);
    appService.addService(savedService);
    serviceTemplateService.createDefaultTemplatesByService(savedService);
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(savedService.getAppId())
            .withDisplayText(getDecoratedNotificationMessage(ENTITY_CREATE_NOTIFICATION,
                ImmutableMap.of("ENTITY_TYPE", "Service", "ENTITY_NAME", savedService.getName())))
            .build());
    return savedService;
  }

  private Service addDefaultCommands(Service service) {
    addCommand(service.getAppId(), service.getUuid(), getInstallCommandGraph());
    addCommand(service.getAppId(), service.getUuid(), getStopCommandGraph());
    return addCommand(service.getAppId(), service.getUuid(), getStartCommandGraph());
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
    if (service == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Service doesn't exist");
    }

    service.setConfigFiles(configService.getConfigFilesForEntity(appId, DEFAULT_TEMPLATE_ID, service.getUuid()));
    service.setLastDeploymentActivity(activityService.getLastActivityForService(appId, serviceId));
    service.setLastProdDeploymentActivity(activityService.getLastProductionActivityForService(appId, serviceId));
    return service;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String appId, String serviceId) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    if (service == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Service doesn't exist");
    }

    boolean deleted = wingsPersistence.delete(Service.class, serviceId);
    if (deleted) {
      executorService.submit(() -> {
        notificationService.sendNotificationAsync(
            anInformationNotification()
                .withAppId(service.getAppId())
                .withDisplayText(getDecoratedNotificationMessage(ENTITY_DELETE_NOTIFICATION,
                    ImmutableMap.of("ENTITY_TYPE", "Service", "ENTITY_NAME", service.getName())))
                .build());
        serviceTemplateService.deleteByService(appId, serviceId);
        configService.deleteByEntityId(appId, serviceId, DEFAULT_TEMPLATE_ID);
      });
    }
  }

  @Override
  public void deleteByAppId(String appId) {
    wingsPersistence.createQuery(Service.class)
        .field("appId")
        .equal(appId)
        .asList()
        .forEach(service -> delete(appId, service.getUuid()));
  }

  @Override
  public List<Service> findServicesByApp(String appId) {
    return wingsPersistence.createQuery(Service.class).field("appId").equal(appId).asList();
  }

  @Override
  public Service get(String appId, String serviceId, SetupStatus status) {
    Service service = get(appId, serviceId);
    if (status == INCOMPLETE) {
      service.setSetup(setupService.getServiceSetupStatus(service));
    }
    return service;
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
  public Service updateCommand(String appId, String serviceId, Graph commandGraph) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    Validator.notNullCheck("service", service);

    if (!commandGraph.isLinear()) {
      throw new IllegalArgumentException("Graph is not a pipeline");
    }

    Command command = aCommand().withGraph(commandGraph).build();
    command.transformGraph();

    wingsPersistence.update(wingsPersistence.createQuery(Service.class)
                                .field(ID_KEY)
                                .equal(serviceId)
                                .field("appId")
                                .equal(appId)
                                .field("commands.name")
                                .equal(command.getName()),
        wingsPersistence.createUpdateOperations(Service.class).set("commands.$", command));

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
  public List<Stencil> getCommandStencils(@NotEmpty String appId, @NotEmpty String serviceId) {
    return stencilPostProcessor.postProcess(Arrays.asList(CommandUnitType.values()), appId, serviceId);
  }

  @Override
  public Map<String, String> getData(String appId, String... params) {
    Service service = get(appId, params[0]);
    if (isEmpty(service.getCommands())) {
      return emptyMap();
    } else {
      return service.getCommands().stream().collect(toMap(Command::getName, Command::getName));
    }
  }
}
