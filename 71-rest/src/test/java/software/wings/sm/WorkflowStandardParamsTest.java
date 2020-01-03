package software.wings.sm;

import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.config.ArtifactSourceable.ARTIFACT_SOURCE_REGISTRY_URL_KEY;
import static software.wings.beans.config.ArtifactSourceable.ARTIFACT_SOURCE_USER_NAME_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_KEY;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.COMPANY_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.mockChecker;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.MembersInjector;

import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.limits.LimitCheckerFactory;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.api.ServiceElement;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.artifact.Artifact;
import software.wings.scheduler.BackgroundJobScheduler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Class WorkflowStandardParamsTest.
 *
 * @author Rishi
 */
public class WorkflowStandardParamsTest extends WingsBaseTest {
  /**
   * The App service.
   */
  @Inject @InjectMocks AppService appService;
  /**
   * The Environment service.
   */
  @Inject EnvironmentService environmentService;
  /**
   * The Injector.
   */
  @Inject MembersInjector<WorkflowStandardParams> injector;

  @Mock SettingsService settingsService;
  @Mock private BackgroundJobScheduler jobScheduler;
  @Mock private ExecutionContext context;
  @Mock private ArtifactService artifactService;
  @Mock private AccountService accountService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Mock private LimitCheckerFactory limitCheckerFactory;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private FeatureFlagService featureFlagService;

  @Before
  public void setup() {
    when(settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, SettingVariableTypes.APP_DYNAMICS.name()))
        .thenReturn(Lists.newArrayList(aSettingAttribute().withUuid("id").build()));
    on(appService).set("settingsService", settingsService);
    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(false);
  }

  /**
   * Should get app.
   */
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldGetApp() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    Application app = anApplication().name("AppA").accountId(ACCOUNT_ID).build();
    app = appService.save(app);

    Environment env = anEnvironment().appId(app.getUuid()).name("DEV").build();
    env = environmentService.save(env);
    app = appService.get(app.getUuid());

    WorkflowStandardParams std = new WorkflowStandardParams();
    injector.injectMembers(std);
    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());

    ServiceElement serviceElement = ServiceElement.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build();
    when(context.getContextElement(ContextElementType.SERVICE)).thenReturn(serviceElement);

    Map<String, Object> map = std.paramMap(context);
    assertThat(map).isNotNull().containsEntry(ContextElement.APP, app).containsEntry(ContextElement.ENV, env);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredApp() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());
    Application app = prepareApp();
    WorkflowStandardParams std = new WorkflowStandardParams();
    injector.injectMembers(std);
    std.setAppId(app.getUuid());
    Application fetchedApp = std.fetchRequiredApp();
    assertThat(fetchedApp).isNotNull();
    assertThat(fetchedApp.getName()).isEqualTo(app.getName());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldThrowOnFetchRequiredApp() {
    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(null);
    std.fetchRequiredApp();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredEnv() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());
    Application app = prepareApp();
    Environment env = prepareEnv(app.getUuid());
    WorkflowStandardParams std = new WorkflowStandardParams();
    injector.injectMembers(std);
    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());
    Environment fetchedEnv = std.fetchRequiredEnv();
    assertThat(fetchedEnv).isNotNull();
    assertThat(fetchedEnv.getName()).isEqualTo(env.getName());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldThrowOnFetchRequiredEnv() {
    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setEnvId(null);
    std.fetchRequiredEnv();
  }

  private Application prepareApp() {
    return appService.save(anApplication().name("app_name").accountId(ACCOUNT_ID).build());
  }

  private Environment prepareEnv(String appId) {
    return environmentService.save(anEnvironment().appId(appId).name("env_name").build());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetArtifactForService() {
    when(artifactService.get(ARTIFACT_ID))
        .thenReturn(
            anArtifact().withUuid(ARTIFACT_ID).withAppId(APP_ID).withArtifactStreamId(ARTIFACT_STREAM_ID).build());
    when(artifactStreamServiceBindingService.listArtifactStreamIds(SERVICE_ID))
        .thenReturn(singletonList(ARTIFACT_STREAM_ID));
    WorkflowStandardParams std = new WorkflowStandardParams();
    injector.injectMembers(std);
    on(std).set("artifactService", artifactService);
    on(std).set("artifactStreamServiceBindingService", artifactStreamServiceBindingService);
    std.setAppId(APP_ID);
    std.setArtifactIds(singletonList(ARTIFACT_ID));

    Artifact artifact = std.getArtifactForService(SERVICE_ID);
    assertThat(artifact).isNotNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetNullArtifact() {
    when(artifactService.get(ARTIFACT_ID)).thenReturn(anArtifact().withUuid(ARTIFACT_ID).withAppId(APP_ID).build());
    when(artifactStreamServiceBindingService.listArtifactStreamIds(SERVICE_ID)).thenReturn(new ArrayList<>());
    WorkflowStandardParams std = new WorkflowStandardParams();
    injector.injectMembers(std);
    on(std).set("artifactService", artifactService);
    on(std).set("artifactStreamServiceBindingService", artifactStreamServiceBindingService);
    std.setAppId(APP_ID);
    std.setArtifactIds(singletonList(ARTIFACT_ID));

    Artifact artifact = std.getArtifactForService(SERVICE_ID);
    assertThat(artifact).isNull();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetArtifactWithSourceProperties() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    Application app = anApplication().name("AppA").accountId(ACCOUNT_ID).build();
    app = appService.save(app);
    Environment env = anEnvironment().appId(app.getUuid()).name("DEV").build();
    env = environmentService.save(env);

    WorkflowStandardParams std = new WorkflowStandardParams();
    injector.injectMembers(std);
    on(std).set("artifactService", artifactService);
    on(std).set("artifactStreamService", artifactStreamService);
    on(std).set("artifactStreamServiceBindingService", artifactStreamServiceBindingService);
    on(std).set("featureFlagService", featureFlagService);

    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());
    List<String> artifactIds = new ArrayList<>();
    artifactIds.add(ARTIFACT_ID);
    std.setArtifactIds(artifactIds);

    ServiceElement serviceElement = ServiceElement.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build();
    when(context.getContextElement(ContextElementType.SERVICE)).thenReturn(serviceElement);
    when(artifactService.get(ARTIFACT_ID))
        .thenReturn(anArtifact()
                        .withUuid(ARTIFACT_ID)
                        .withAppId(app.getUuid())
                        .withArtifactStreamId(ARTIFACT_STREAM_ID)
                        .build());
    when(artifactStreamService.fetchArtifactSourceProperties(app.getAccountId(), ARTIFACT_STREAM_ID))
        .thenReturn(ImmutableMap.of(
            ARTIFACT_SOURCE_USER_NAME_KEY, "harness", ARTIFACT_SOURCE_REGISTRY_URL_KEY, "http://docker.registry.io"));
    when(artifactStreamServiceBindingService.listArtifactStreamIds(SERVICE_ID))
        .thenReturn(singletonList(ARTIFACT_STREAM_ID));

    Map<String, Object> map = std.paramMap(context);
    assertThat(map).isNotNull().containsKey(ContextElement.ARTIFACT);
    Artifact artifact = (Artifact) map.get(ContextElement.ARTIFACT);
    assertThat(artifact).isNotNull();
    assertThat(artifact.getSource())
        .isNotEmpty()
        .containsKeys(ARTIFACT_SOURCE_USER_NAME_KEY, ARTIFACT_SOURCE_REGISTRY_URL_KEY);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetAccountDefaults() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    Application app = anApplication().name("AppA").accountId(ACCOUNT_ID).build();
    app = appService.save(app);

    Environment env = anEnvironment().appId(app.getUuid()).name("DEV").build();
    env = environmentService.save(env);

    WorkflowStandardParams std = new WorkflowStandardParams();
    injector.injectMembers(std);
    on(std).set("accountService", accountService);
    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());

    when(accountService.getAccountWithDefaults(ACCOUNT_ID))
        .thenReturn(Account.Builder.anAccount()
                        .withAccountKey(ACCOUNT_KEY)
                        .withCompanyName(COMPANY_NAME)
                        .withUuid(ACCOUNT_ID)
                        .withDefaults(ImmutableMap.of("MyActDefaultVar", "MyDefaultValue"))
                        .build());

    Map<String, Object> map = std.paramMap(context);
    assertThat(map).isNotNull().containsKey(ContextElement.ACCOUNT);
    Account account = (Account) map.get(ContextElement.ACCOUNT);
    assertThat(account).isNotNull();
    assertThat(account.getDefaults()).isNotEmpty();
  }
}
