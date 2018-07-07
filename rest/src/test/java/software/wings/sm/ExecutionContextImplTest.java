package software.wings.sm;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.when;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;

import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Key;
import software.wings.WingsBaseTest;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.HostElement;
import software.wings.api.HttpStateExecutionData;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.artifact.Artifact;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

/**
 * The Class ExecutionContextImplTest.
 *
 * @author Rishi
 */
public class ExecutionContextImplTest extends WingsBaseTest {
  /**
   * The Injector.
   */
  @Inject Injector injector;
  /**
   * The App service.
   */
  @Inject @InjectMocks AppService appService;
  /**
   * The Environment service.
   */
  @Inject EnvironmentService environmentService;

  @Mock private JobScheduler jobScheduler;

  @Mock private SettingsService settingsService;

  @Mock private ArtifactService artifactService;

  @Mock private ServiceTemplateService serviceTemplateService;

  @Before
  public void setup() {
    when(settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, SettingVariableTypes.APP_DYNAMICS.name()))
        .thenReturn(Lists.newArrayList(aSettingAttribute().withUuid("id").build()));
    on(appService).set("settingsService", settingsService);
  }

  /**
   * Should fetch context element.
   */
  @Test
  public void shouldFetchContextElement() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();

    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);

    ServiceElement element1 = new ServiceElement();
    element1.setUuid(generateUuid());
    element1.setName("svc1");
    context.pushContextElement(element1);

    ServiceElement element2 = new ServiceElement();
    element2.setUuid(generateUuid());
    element2.setName("svc2");
    context.pushContextElement(element2);

    ServiceElement element3 = new ServiceElement();
    element3.setUuid(generateUuid());
    element3.setName("svc3");
    context.pushContextElement(element3);

    ServiceElement element = context.getContextElement(ContextElementType.SERVICE);
    assertThat(element).isNotNull().isEqualToComparingFieldByField(element3);
  }

  /**
   * Should fetch context element.
   */
  @Test
  public void shouldRenderExpression() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setDisplayName("abc");
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    injector.injectMembers(context);

    ServiceElement svc = new ServiceElement();
    svc.setUuid(generateUuid());
    svc.setName("svc1");
    context.pushContextElement(svc);

    ServiceTemplateElement st = new ServiceTemplateElement();
    st.setUuid(generateUuid());
    st.setName("st1");
    st.setServiceElement(aServiceElement().withUuid(generateUuid()).withName("svc2").build());
    context.pushContextElement(st);

    HostElement host = new HostElement();
    host.setUuid(generateUuid());
    host.setHostName("host1");
    context.pushContextElement(host);

    Application app = anApplication().withName("AppA").withAccountId(ACCOUNT_ID).build();
    app = appService.save(app);

    Environment env = anEnvironment().withAppId(app.getUuid()).withName("DEV").build();
    env = environmentService.save(env);

    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());

    String timeStampId = std.getTimestampId();

    injector.injectMembers(std);
    context.pushContextElement(std);

    String expr =
        "$HOME/${env.name}/${app.name}/${service.name}/${serviceTemplate.name}/${host.name}/${timestampId}/runtime";
    String path = context.renderExpression(expr);
    assertThat(path).isEqualTo("$HOME/DEV/AppA/svc2/st1/host1/" + timeStampId + "/runtime");
  }

  /**
   * Should evaluate indirect references
   */
  @Test
  public void shouldEvaluateIndirectExpression() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setExecutionUuid(generateUuid());
    stateExecutionInstance.setDisplayName("http");
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    injector.injectMembers(context);

    ServiceElement svc = new ServiceElement();
    svc.setUuid(generateUuid());
    svc.setName("svc1");
    context.pushContextElement(svc);

    ServiceTemplateElement st = new ServiceTemplateElement();
    st.setUuid(generateUuid());
    st.setName("st1");
    st.setServiceElement(aServiceElement().withUuid(generateUuid()).withName("svc2").build());
    context.pushContextElement(st);

    HostElement host = new HostElement();
    host.setUuid(generateUuid());
    host.setHostName("host1");
    context.pushContextElement(host);

    Application app = anApplication().withName("AppA").withAccountId(ACCOUNT_ID).build();
    app = appService.save(app);

    Environment env = anEnvironment().withAppId(app.getUuid()).withName("DEV").build();
    env = environmentService.save(env);

    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());
    std.setArtifactIds(asList(ARTIFACT_ID));

    Artifact artifact = Artifact.Builder.anArtifact()
                            .withServiceIds(asList(svc.getUuid()))
                            .withMetadata(Maps.newHashMap("buildNo", "123-SNAPSHOT"))
                            .build();
    String timeStampId = std.getTimestampId();

    injector.injectMembers(std);
    context.pushContextElement(std);

    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .serviceId(svc.getUuid())
                                          .name("REV")
                                          .value("${artifact.buildNo}".toCharArray())
                                          .build();
    when(serviceTemplateService.computeServiceVariables(
             app.getUuid(), env.getUuid(), st.getUuid(), context.getWorkflowExecutionId(), false))
        .thenReturn(asList(serviceVariable));
    when(serviceTemplateService.getTemplateRefKeysByService(app.getUuid(), svc.getUuid(), env.getUuid()))
        .thenReturn(asList(new Key(ServiceTemplate.class, "serviceTemplates", st.getUuid())));
    when(artifactService.get(app.getUuid(), ARTIFACT_ID)).thenReturn(artifact);
    on(std).set("artifactService", artifactService);
    on(std).set("serviceTemplateService", serviceTemplateService);

    String expr = "${httpResponseBody}.contains(${serviceVariable.REV})";
    HttpStateExecutionData httpStateExecutionData =
        HttpStateExecutionData.builder().httpResponseBody("abcabcabcabcabc-123-SNAPSHOT-23423sadf").build();
    boolean assertion = (boolean) context.evaluateExpression(expr, httpStateExecutionData);
    assertThat(assertion).isTrue();
  }

  /**
   * Should evaluate indirect references
   */
  @Test
  public void shouldRenderTemplateVariableExpression() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setExecutionUuid(generateUuid());
    stateExecutionInstance.setDisplayName("http");
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    injector.injectMembers(context);

    ServiceElement svc = new ServiceElement();
    svc.setUuid(generateUuid());
    svc.setName("svc1");
    context.pushContextElement(svc);

    ServiceTemplateElement st = new ServiceTemplateElement();
    st.setUuid(generateUuid());
    st.setName("st1");
    st.setServiceElement(aServiceElement().withUuid(generateUuid()).withName("svc2").build());
    context.pushContextElement(st);

    HostElement host = new HostElement();
    host.setUuid(generateUuid());
    host.setHostName("host1");
    context.pushContextElement(host);

    Application app = anApplication().withName("AppA").withAccountId(ACCOUNT_ID).build();
    app = appService.save(app);

    Environment env = anEnvironment().withAppId(app.getUuid()).withName("DEV").build();
    env = environmentService.save(env);

    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());
    std.setArtifactIds(asList(ARTIFACT_ID));

    Artifact artifact = Artifact.Builder.anArtifact()
                            .withServiceIds(asList(svc.getUuid()))
                            .withMetadata(Maps.newHashMap("buildNo", "123-SNAPSHOT"))
                            .build();

    injector.injectMembers(std);
    context.pushContextElement(std);

    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .serviceId(svc.getUuid())
                                          .name("REV")
                                          .value("${artifact.buildNo}".toCharArray())
                                          .build();
    when(serviceTemplateService.computeServiceVariables(
             app.getUuid(), env.getUuid(), st.getUuid(), context.getWorkflowExecutionId(), false))
        .thenReturn(asList(serviceVariable));
    when(serviceTemplateService.getTemplateRefKeysByService(app.getUuid(), svc.getUuid(), env.getUuid()))
        .thenReturn(asList(new Key(ServiceTemplate.class, "serviceTemplates", st.getUuid())));
    when(artifactService.get(app.getUuid(), ARTIFACT_ID)).thenReturn(artifact);
    on(std).set("artifactService", artifactService);
    on(std).set("serviceTemplateService", serviceTemplateService);

    CommandStateExecutionData commandStateExecutionData = CommandStateExecutionData.Builder.aCommandStateExecutionData()
                                                              .withTemplateVariable(ImmutableMap.of("MyVar", "MyValue"))
                                                              .build();
    String expr = "echo ${MyVar}-${artifact.buildNo}";

    String evaluatedExpression = context.renderExpression(expr, commandStateExecutionData, artifact);
    assertThat(evaluatedExpression).isNotEmpty();
    assertThat(evaluatedExpression).isEqualTo("echo MyValue-123-SNAPSHOT");
  }
}
