package software.wings.integration;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.InfrastructureMappingRule.Builder.anInfrastructureMappingRule;
import static software.wings.beans.InfrastructureMappingRule.HostRuleOperator.EQUAL;
import static software.wings.beans.InfrastructureMappingRule.HostRuleOperator.STARTS_WITH;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.infrastructure.AwsInfrastructureProviderConfig.Builder.anAwsInfrastructureProviderConfig;
import static software.wings.beans.infrastructure.Host.Builder.aHost;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.wings.WingsBaseTest;
import software.wings.beans.AppDynamicsConfig;
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
import software.wings.service.intfc.SettingsService;

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

  @Inject private SettingsService settingsService;

  /**
   * The Infrastructure id.
   */
  private String infraId;

  /**
   * The Environment.
   */
  private Environment environment;

  /**
   * The Order service template.
   */
  private ServiceTemplate orderServiceTemplate;
  /**
   * The Account service template.
   */
  private ServiceTemplate accountServiceTemplate;

  /**
   * The Setting attribute.
   */
  private SettingAttribute settingAttribute;

  /**
   * Sets the up.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Before
  public void setUp() throws IOException {
    String accountId = wingsPersistence.save(anAccount().withCompanyName("Wings Software").build());

    settingsService.save(aSettingAttribute()
                             .withIsPluginSetting(true)
                             .withName("AppDynamics")
                             .withAccountId(accountId)
                             .withValue(AppDynamicsConfig.Builder.anAppDynamicsConfig()
                                            .withControllerUrl("https://na774.saas.appdynamics.com/controller")
                                            .withUsername("testuser")
                                            .withAccountname("na774")
                                            .withPassword("testuser123")
                                            .build())
                             .build());

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

    Host baseHost =
        aHost().withAppId(GLOBAL_APP_ID).withInfraId(savedInfra.getUuid()).withHostName("aws.host1").build();
    Host host = wingsPersistence.saveAndGet(Host.class, baseHost);

    infrastructureService.applyApplicableMappingRules(infrastructure, host);
    List<ApplicationHost> appHosts =
        hostService.list(PageRequest.Builder.aPageRequest().addFilter("appId", EQ, environment.getAppId()).build())
            .getResponse();
    assertThat(appHosts).hasSize(1);
  }

  /**
   * Should sync aws host.
   */
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

  @Test
  @Ignore
  public void shouldSyncAwsInfraHost() {
    SettingAttribute awsCredentials =
        settingsService.save(aSettingAttribute()
                                 .withName("AWS_CREDENTIALS")
                                 .withValue(anAwsInfrastructureProviderConfig()
                                                .withAccessKey("AKIAI6IK4KYQQQEEWEVA")
                                                .withSecretKey("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")
                                                .build())
                                 .build());

    Infrastructure infrastructure =
        Infrastructure.Builder.anInfrastructure()
            .withAppId(GLOBAL_APP_ID)
            .withInfrastructureConfigId(awsCredentials.getUuid())
            .withType(InfrastructureType.AWS)
            .withInfrastructureMappingRules(asList(anInfrastructureMappingRule()
                                                       .withAppId("ALL")
                                                       .withRules(asList(new Rule("HOST_NAME", STARTS_WITH, "ip-")))
                                                       .build()))
            .build();
    Infrastructure savedInfra = infrastructureService.save(infrastructure);

    infrastructureService.sync(savedInfra.getUuid());

    List<Host> hosts = wingsPersistence.createQuery(Host.class).asList();
    List<ApplicationHost> applicationHosts =
        wingsPersistence.createQuery(ApplicationHost.class).field("infraId").equal(infrastructure.getUuid()).asList();
    int count = (int) wingsPersistence.createQuery(Environment.class).countAll();

    Assertions.assertThat(hosts.size()).isEqualTo(19);
    Assertions.assertThat(applicationHosts.size()).isEqualTo(hosts.size() * count);
  }
}
