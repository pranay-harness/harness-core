package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.InfrastructureMappingRule.Builder.anInfrastructureMappingRule;
import static software.wings.beans.InfrastructureMappingRule.HostRuleOperator.CONTAINS;
import static software.wings.beans.infrastructure.HostUsage.Builder.aHostUsage;
import static software.wings.beans.infrastructure.StaticInfrastructure.Builder.aStaticInfrastructure;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.ErrorCodes;
import software.wings.beans.Host;
import software.wings.beans.InfrastructureMappingRule;
import software.wings.beans.InfrastructureMappingRule.Rule;
import software.wings.beans.infrastructure.ApplicationHostUsage;
import software.wings.beans.infrastructure.HostUsage;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureService;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class InfrastructureServiceImpl.
 */
@ValidateOnExecution
@Singleton
public class InfrastructureServiceImpl implements InfrastructureService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HostService hostService;
  @Inject private ExecutorService executorService;
  @Inject private AppService appService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.InfrastructureService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Infrastructure> list(PageRequest<Infrastructure> req, boolean overview) {
    PageResponse<Infrastructure> infrastructures = wingsPersistence.query(Infrastructure.class, req);
    if (overview) {
      infrastructures.getResponse().forEach(
          infrastructure -> { infrastructure.setHostUsage(getInfrastructureHostUsage(infrastructure)); });
    }
    return infrastructures;
  }

  private HostUsage getInfrastructureHostUsage(Infrastructure infrastructure) {
    int totalCount = hostService.getHostCountByInfrastructure(infrastructure.getUuid());
    List<ApplicationHostUsage> applicationHostUsages =
        hostService.getInfrastructureHostUsageByApplication(infrastructure.getUuid());
    return aHostUsage().withTotalCount(totalCount).withApplicationHosts(applicationHostUsages).build();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.InfrastructureService#save(software.wings.beans.infrastructure.Infrastructure)
   */
  @Override
  public Infrastructure save(Infrastructure infrastructure) {
    return wingsPersistence.saveAndGet(Infrastructure.class, infrastructure);
  }

  @Override
  public void delete(String infraId) {
    boolean deleted = wingsPersistence.delete(wingsPersistence.createQuery(Infrastructure.class)
                                                  .field("appId")
                                                  .equal(Base.GLOBAL_APP_ID)
                                                  .field(ID_KEY)
                                                  .equal(infraId));
    if (deleted) {
      executorService.submit(() -> hostService.deleteByInfra(infraId));
    }
  }

  @Override
  public Infrastructure getInfraByEnvId(String appId, String envId) {
    // TODO:: INFRA
    return wingsPersistence.createQuery(Infrastructure.class).field("appId").equal(GLOBAL_APP_ID).get();
  }

  @Override
  public void createDefaultInfrastructure(String appId) {
    List<Infrastructure> infrastructures =
        wingsPersistence.createQuery(Infrastructure.class).field("appId").equal(GLOBAL_APP_ID).asList();
    if (infrastructures.size() == 0) {
      wingsPersistence.save(
          aStaticInfrastructure()
              .withAppId(GLOBAL_APP_ID)
              .withInfrastructureMappingRules(asList(anInfrastructureMappingRule()
                                                         .withAppId("ALL")
                                                         .withRules(asList(new Rule("HOST_NAME", CONTAINS, "aws")))
                                                         .build()))
              .withName("Static Infrastructure")
              .build());
    }
  }

  @Override
  public String getDefaultInfrastructureId() {
    List<Infrastructure> infrastructures =
        wingsPersistence.createQuery(Infrastructure.class).field("appId").equal(GLOBAL_APP_ID).asList();
    if (infrastructures.size() == 0) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST);
    }
    return infrastructures.get(0).getUuid();
  }

  @Override
  public Infrastructure get(String infraId) {
    Infrastructure infrastructure = wingsPersistence.createQuery(Infrastructure.class)
                                        .field("appId")
                                        .equal(Base.GLOBAL_APP_ID)
                                        .field(ID_KEY)
                                        .equal(infraId)
                                        .get();
    Validator.notNullCheck("Infrastructure", infrastructure);
    return infrastructure;
  }

  @Override
  public PageRequest<Host> listInfraHost(PageRequest<Host> pageRequest) {
    return wingsPersistence.query(Host.class, pageRequest);
  }

  @Override
  public void applyApplicableMappingRules(Infrastructure infrastructure, Host host) {
    List<InfrastructureMappingRule> infrastructureMappingRules = infrastructure.getInfrastructureMappingRules();
    infrastructureMappingRules.forEach(rule -> applyInfraMapping(rule, host));
  }

  private void applyInfraMapping(InfrastructureMappingRule mappingRule, Host host) {
    Map<String, String> context = getHostAttributes(host);
    if (mappingRule.evaluate(context)) {
      List<Application> applications = getApplicableApplications(mappingRule);
      applications.stream()
          .filter(Objects::nonNull)
          .forEach(application
              -> hostService.addApplicationHost(
                  application.getAppId(), mappingRule.getEnvId(), mappingRule.getTagId(), host));
    }
  }

  private List<Application> getApplicableApplications(InfrastructureMappingRule mappingRule) {
    return "ALL".equals(mappingRule.getAppId()) ? appService.list(aPageRequest().build(), false, 0).getResponse()
                                                : asList(appService.get(mappingRule.getAppId()));
  }

  private Map<String, String> getHostAttributes(Host host) {
    return ImmutableMap.of("HOST_NAME", host.getHostName());
  }
}
