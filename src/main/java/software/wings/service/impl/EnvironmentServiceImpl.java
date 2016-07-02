package software.wings.service.impl;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Environment.EnvironmentBuilder.anEnvironment;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.Environment;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;
import software.wings.stencils.DataProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/1/16.
 */

@ValidateOnExecution
@Singleton
public class EnvironmentServiceImpl implements EnvironmentService, DataProvider {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private InfraService infraService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private TagService tagService;
  @Inject private ExecutorService executorService;
  @Inject private AppService appService;

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Environment> list(PageRequest<Environment> request) {
    return wingsPersistence.query(Environment.class, request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> getData(String appId, String... params) {
    PageRequest<Environment> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", appId, Operator.EQ);
    return list(pageRequest).stream().collect(toMap(Environment::getUuid, Environment::getName));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment save(Environment env) {
    env = wingsPersistence.saveAndGet(Environment.class, env);
    appService.addEnvironment(env);
    infraService.createDefaultInfraForEnvironment(env.getAppId(), env.getUuid()); // FIXME: stopgap for Alpha
    tagService.createDefaultRootTagForEnvironment(env);
    serviceTemplateService.createDefaultTemplatesByEnv(env);
    return env;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment get(String appId, String envId) {
    return wingsPersistence.get(Environment.class, appId, envId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment update(Environment environment) {
    wingsPersistence.updateFields(Environment.class, environment.getUuid(),
        ImmutableMap.of("name", environment.getName(), "description", environment.getDescription()));
    return wingsPersistence.get(Environment.class, environment.getAppId(), environment.getUuid());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String appId, String envId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).field(ID_KEY).equal(envId));
    executorService.submit(() -> {
      serviceTemplateService.deleteByEnv(appId, envId);
      tagService.deleteByEnv(appId, envId);
      infraService.deleteByEnv(appId, envId);
    });
  }

  @Override
  public void deleteByApp(String appId) {
    List<Environment> environments =
        wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
    environments.forEach(environment -> delete(appId, environment.getUuid()));
  }

  @Override
  public void createDefaultEnvironments(String appId) {
    asList("DEV", "QA", "PROD", "UAT").forEach(name -> save(anEnvironment().withAppId(appId).withName(name).build()));
  }

  @Override
  public List<Environment> getEnvByApp(String appId) {
    return wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
  }
}
