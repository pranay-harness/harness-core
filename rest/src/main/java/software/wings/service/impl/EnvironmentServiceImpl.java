package software.wings.service.impl;

import static java.util.stream.Collectors.toMap;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.SearchFilter;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Setup.SetupStatus;
import software.wings.common.Constants;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.WorkflowService;
import software.wings.stencils.DataProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 4/1/16.
 */
@ValidateOnExecution
@Singleton
public class EnvironmentServiceImpl implements EnvironmentService, DataProvider {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ExecutorService executorService;
  @Inject private WorkflowService workflowService;
  @Inject private SetupService setupService;
  @Inject private NotificationService notificationService;
  @Inject private ActivityService activityService;
  @Inject private PipelineService pipelineService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Environment> list(PageRequest<Environment> request, boolean withSummary) {
    PageResponse<Environment> pageResponse = wingsPersistence.query(Environment.class, request);
    if (withSummary) {
      pageResponse.getResponse().forEach(environment -> { addServiceTemplates(environment); });
    }
    return pageResponse;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment get(String appId, String envId, boolean withSummary) {
    Environment environment = wingsPersistence.get(Environment.class, appId, envId);
    if (environment == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Environment doesn't exist");
    }
    if (withSummary) {
      addServiceTemplates(environment);
    }
    return environment;
  }

  @Override
  public Environment get(@NotEmpty String appId, @NotEmpty String envId, @NotNull SetupStatus status) {
    Environment environment = get(appId, envId, true);
    if (status == SetupStatus.INCOMPLETE) {
      environment.setSetup(setupService.getEnvironmentSetupStatus(environment));
    }
    return environment;
  }

  @Override
  public boolean exist(@NotEmpty String appId, @NotEmpty String envId) {
    return wingsPersistence.createQuery(Environment.class)
               .field("appId")
               .equal(appId)
               .field(ID_KEY)
               .equal(envId)
               .getKey()
        != null;
  }

  private void addServiceTemplates(Environment environment) {
    PageRequest<ServiceTemplate> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", environment.getAppId(), SearchFilter.Operator.EQ);
    pageRequest.addFilter("envId", environment.getUuid(), EQ);
    environment.setServiceTemplates(serviceTemplateService.list(pageRequest, false, false).getResponse());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> getData(String appId, String... params) {
    PageRequest<Environment> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", appId, EQ);
    return list(pageRequest, false).stream().collect(toMap(Environment::getUuid, Environment::getName));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment save(Environment environment) {
    environment = wingsPersistence.saveAndGet(Environment.class, environment);
    serviceTemplateService.createDefaultTemplatesByEnv(environment);
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(environment.getAppId())
            .withNotificationTemplateId(NotificationMessageType.ENTITY_CREATE_NOTIFICATION.name())
            .withNotificationTemplateVariables(
                ImmutableMap.of("ENTITY_TYPE", "Environment", "ENTITY_NAME", environment.getName()))
            .build());
    return environment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment update(Environment environment) {
    String description = environment.getDescription() == null ? "" : environment.getDescription();
    ImmutableMap<String, Object> paramMap = ImmutableMap.of(
        "name", environment.getName(), "environmentType", environment.getEnvironmentType(), "description", description);

    wingsPersistence.updateFields(Environment.class, environment.getUuid(), paramMap);
    return wingsPersistence.get(Environment.class, environment.getAppId(), environment.getUuid());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String appId, String envId) {
    Environment environment = wingsPersistence.get(Environment.class, appId, envId);
    if (environment == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Environment doesn't exist");
    }
    ensureEnvironmentSafeToDelete(environment);
    delete(environment);
  }

  private void ensureEnvironmentSafeToDelete(Environment environment) {
    List<Pipeline> pipelines = pipelineService.listPipelines(
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, environment.getAppId())
            .addFilter("pipelineStages.pipelineStageElements.properties.envId", EQ, environment.getUuid())
            .build());

    if (pipelines.size() > 0) {
      List<String> pipelineNames = pipelines.stream().map(Pipeline::getName).collect(Collectors.toList());
      throw new WingsException(INVALID_REQUEST, "message",
          String.format("Environment is referenced by %s pipline%s [%s].", pipelines.size(),
              pipelines.size() == 1 ? "" : "s", Joiner.on(", ").join(pipelineNames)));
    }
  }

  private void delete(Environment environment) {
    boolean deleted = wingsPersistence.delete(environment);

    if (deleted) {
      executorService.submit(() -> serviceTemplateService.deleteByEnv(environment.getAppId(), environment.getUuid()));
      executorService.submit(
          () -> workflowService.deleteWorkflowByEnvironment(environment.getAppId(), environment.getUuid()));
      executorService.submit(() -> activityService.deleteByEnvironment(environment.getAppId(), environment.getUuid()));
      notificationService.sendNotificationAsync(
          anInformationNotification()
              .withAppId(environment.getAppId())
              .withNotificationTemplateId(NotificationMessageType.ENTITY_DELETE_NOTIFICATION.name())
              .withNotificationTemplateVariables(
                  ImmutableMap.of("ENTITY_TYPE", "Environment", "ENTITY_NAME", environment.getName()))
              .build());
    }
  }

  @Override
  public void deleteByApp(String appId) {
    List<Environment> environments =
        wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
    environments.forEach(this ::delete);
  }

  @Override
  public void createDefaultEnvironments(String appId) {
    save(anEnvironment().withAppId(appId).withName(Constants.DEV_ENV).withEnvironmentType(NON_PROD).build());
    save(anEnvironment().withAppId(appId).withName(Constants.QA_ENV).withEnvironmentType(NON_PROD).build());
    save(anEnvironment().withAppId(appId).withName(Constants.PROD_ENV).withEnvironmentType(PROD).build());
  }

  @Override
  public List<Environment> getEnvByApp(String appId) {
    return wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
  }
}
