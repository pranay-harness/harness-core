package software.wings.integration;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.InfrastructureMappingRule.Builder.anInfrastructureMappingRule;
import static software.wings.beans.InfrastructureMappingRule.HostRuleOperator.EQUAL;
import static software.wings.beans.InfrastructureMappingRule.HostRuleOperator.STARTS_WITH;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.infrastructure.Host.Builder.aHost;

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMappingRule;
import software.wings.beans.InfrastructureMappingRule.Rule;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.ApplicationHost;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.beans.infrastructure.Infrastructure.InfrastructureType;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.rules.RealMongo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureService;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 10/4/16.
 */

@RealMongo
public class InfraIntegrationTest extends WingsBaseTest {
  /**
   * The Test folder.
   */
  @org.junit.Rule public TemporaryFolder testFolder = new TemporaryFolder();
  /**
   * The Wings persistence.
   */
  @Inject private WingsPersistence wingsPersistence;
  /**
   * The App service.
   */
  @Inject private AppService appService;

  /**
   * The Infrastructure service.
   */
  @Inject private InfrastructureService infrastructureService;

  /**
   * The Environment service.
   */
  @Inject private EnvironmentService environmentService;
  /**
   * The Host service.
   */
  @Inject private HostService hostService;

  /**
   * The Infrastructure id.
   */
  String infraId;

  /**
   * The Environment.
   */
  Environment environment;

  /**
   * The Order service template.
   */
  ServiceTemplate orderServiceTemplate;
  /**
   * The Account service template.
   */
  ServiceTemplate accountServiceTemplate;

  /**
   * The Setting attribute.
   */
  SettingAttribute settingAttribute;

  /**
   * Sets the up.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Before
  public void setUp() throws IOException {
    Application app = appService.save(anApplication().withName("AppA").build());
    List<Environment> environments = environmentService.getEnvByApp(app.getUuid());
    for (int i = 1; i < environments.size(); i++) {
      environmentService.delete(app.getUuid(), environments.get(i).getUuid());
    }
    environment = environments.get(0);
  }

  /**
   * Should add infra host and apply auto mapping rules.
   */
  @Test
  public void shouldAddInfraHostAndApplyAutoMappingRules() {
    List<Rule> rules = asList(new InfrastructureMappingRule.Rule("HOST_NAME", STARTS_WITH, "aws"));
    List<InfrastructureMappingRule> infrastructureMappingRules = asList(anInfrastructureMappingRule()
                                                                            .withRules(rules)
                                                                            .withAppId(environment.getAppId())
                                                                            .withEnvId(environment.getUuid())
                                                                            .build());

    Infrastructure infrastructure = Infrastructure.Builder.anInfrastructure()
                                        .withType(InfrastructureType.STATIC)
                                        .withAppId(GLOBAL_APP_ID)
                                        .withInfrastructureMappingRules(infrastructureMappingRules)
                                        .build();
    Infrastructure savedInfra = infrastructureService.save(infrastructure);

    Host baseHost = aHost()
                        .withAppId(GLOBAL_APP_ID)
                        .withInfraId(savedInfra.getUuid())
                        .withHostConnAttr(settingAttribute)
                        .withBastionConnAttr(settingAttribute)
                        .withHostName("aws.host1")
                        .build();
    Host host = wingsPersistence.saveAndGet(Host.class, baseHost);

    infrastructureService.applyApplicableMappingRules(infrastructure, host);
    List<ApplicationHost> appHosts =
        hostService.list(PageRequest.Builder.aPageRequest().addFilter("appId", EQ, environment.getAppId()).build())
            .getResponse();
    assertThat(appHosts).hasSize(1);
  }

  @Test
  public void shouldSyncAwsHost() {
    List<Rule> rules = asList(new InfrastructureMappingRule.Rule("NAME", EQUAL, "DemoTargetHosts"));
    List<InfrastructureMappingRule> infrastructureMappingRules =
        asList(anInfrastructureMappingRule().withRules(rules).withAppId("ALL").build());
    Infrastructure infrastructure = Infrastructure.Builder.anInfrastructure()
                                        .withType(InfrastructureType.AWS)
                                        .withAppId(GLOBAL_APP_ID)
                                        .withInfrastructureMappingRules(infrastructureMappingRules)
                                        .build();
    Infrastructure savedInfra = infrastructureService.save(infrastructure);
  }
}
