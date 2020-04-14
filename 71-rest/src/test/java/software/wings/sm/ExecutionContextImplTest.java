package software.wings.sm;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.service.intfc.ServiceTemplateService.EncryptedFieldComputeMode.OBTAIN_META;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.mockChecker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.deployment.InstanceDetails;
import io.harness.exception.InvalidRequestException;
import io.harness.limits.LimitCheckerFactory;
import io.harness.rule.Owner;
import org.assertj.core.util.Maps;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mongodb.morphia.Key;
import software.wings.WingsBaseTest;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.HostElement;
import software.wings.api.HttpStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.api.WorkflowElement;
import software.wings.api.artifact.ServiceArtifactElement;
import software.wings.api.artifact.ServiceArtifactElements;
import software.wings.api.artifact.ServiceArtifactVariableElement;
import software.wings.api.artifact.ServiceArtifactVariableElements;
import software.wings.api.instancedetails.InstanceInfoVariables;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.artifact.Artifact;
import software.wings.scheduler.BackgroundJobScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Class ExecutionContextImplTest.
 *
 * @author Rishi
 */
public class ExecutionContextImplTest extends WingsBaseTest {
  @Inject Injector injector;

  @Inject @InjectMocks AppService appService;
  @Inject EnvironmentService environmentService;

  @Mock private BackgroundJobScheduler jobScheduler;
  @Mock private SettingsService settingsService;
  @Mock private ArtifactService artifactService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private LimitCheckerFactory limitCheckerFactory;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private SweepingOutputService sweepingOutputService;

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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldFetchContextElement() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();

    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);

    ServiceElement element1 = ServiceElement.builder().uuid(generateUuid()).name("svc1").build();
    context.pushContextElement(element1);

    ServiceElement element2 = ServiceElement.builder().uuid(generateUuid()).name("svc2").build();
    context.pushContextElement(element2);

    ServiceElement element3 = ServiceElement.builder().uuid(generateUuid()).name("svc3").build();
    context.pushContextElement(element3);

    ServiceElement element = context.getContextElement(ContextElementType.SERVICE);
    assertThat(element).isNotNull().isEqualToComparingFieldByField(element3);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldFetchWorkflowStandardParams() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();

    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);

    context.pushContextElement(aWorkflowStandardParams()
                                   .withAppId(generateUuid())
                                   .withWorkflowElement(WorkflowElement.builder().build())
                                   .build());

    assertThat(context.fetchWorkflowStandardParamsFromContext()).isNotNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldThrowWhenNoContextElements() {
    ExecutionContextImpl context = new ExecutionContextImpl(new StateExecutionInstance());
    context.fetchWorkflowStandardParamsFromContext();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldThrowWhenNoWorkflowStandardParams() {
    ExecutionContextImpl context = new ExecutionContextImpl(new StateExecutionInstance());
    ServiceElement element1 = ServiceElement.builder().uuid(generateUuid()).name("svc1").build();
    context.pushContextElement(element1);
    context.fetchWorkflowStandardParamsFromContext();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldFetchNoArtifacts() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());
    ExecutionContextImpl context = new ExecutionContextImpl(new StateExecutionInstance());
    injector.injectMembers(context);

    Application app = anApplication().name("AppA").accountId(ACCOUNT_ID).build();
    app = appService.save(app);

    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(false);

    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(app.getUuid());
    std.setArtifactIds(asList(ARTIFACT_ID));
    injector.injectMembers(std);
    context.pushContextElement(std);

    assertThat(context.getArtifacts()).isNullOrEmpty();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFetchArtifactsFromSweepingOutput() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());
    doReturn(asList(ServiceArtifactElements.builder()
                        .artifactElements(asList(ServiceArtifactElement.builder().uuid("u1").build(),
                            ServiceArtifactElement.builder().uuid("u2").build()))
                        .build(),
                 ServiceArtifactElements.builder().artifactElements(emptyList()).build(),
                 ServiceArtifactElements.builder()
                     .artifactElements(asList(ServiceArtifactElement.builder().uuid("u3").build(),
                         ServiceArtifactElement.builder().uuid("u4").build()))
                     .build()))
        .when(sweepingOutputService)
        .findSweepingOutputsWithNamePrefix(any(SweepingOutputInquiry.class), eq(Scope.PIPELINE));
    ExecutionContextImpl context = new ExecutionContextImpl(new StateExecutionInstance());
    injector.injectMembers(context);
    Reflect.on(context).set("sweepingOutputService", sweepingOutputService);
    Reflect.on(context).set("artifactService", artifactService);
    Reflect.on(context).set("featureFlagService", featureFlagService);

    Application app = anApplication().name("AppA").accountId(ACCOUNT_ID).build();
    app = appService.save(app);

    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(false);

    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(app.getUuid());
    injector.injectMembers(std);
    context.pushContextElement(std);

    context.getArtifacts();
    verify(artifactService).get(eq("u1"));
    verify(artifactService).get(eq("u2"));
    verify(artifactService).get(eq("u3"));
    verify(artifactService).get(eq("u4"));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFetchArtifactVariablesFromSweepingOutputFFOn() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());
    doReturn(asList(ServiceArtifactVariableElements.builder()
                        .artifactVariableElements(asList(ServiceArtifactVariableElement.builder().uuid("u1").build(),
                            ServiceArtifactVariableElement.builder().uuid("u2").build()))
                        .build(),
                 ServiceArtifactVariableElements.builder().artifactVariableElements(emptyList()).build(),
                 ServiceArtifactVariableElements.builder()
                     .artifactVariableElements(asList(ServiceArtifactVariableElement.builder().uuid("u3").build(),
                         ServiceArtifactVariableElement.builder().uuid("u4").build()))
                     .build()))
        .when(sweepingOutputService)
        .findSweepingOutputsWithNamePrefix(any(SweepingOutputInquiry.class), eq(Scope.PIPELINE));
    ExecutionContextImpl context = new ExecutionContextImpl(new StateExecutionInstance());
    injector.injectMembers(context);
    Reflect.on(context).set("sweepingOutputService", sweepingOutputService);
    Reflect.on(context).set("artifactService", artifactService);
    Reflect.on(context).set("featureFlagService", featureFlagService);

    Application app = anApplication().name("AppA").accountId(ACCOUNT_ID).build();
    app = appService.save(app);

    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(true);

    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(app.getUuid());
    injector.injectMembers(std);
    context.pushContextElement(std);

    context.getArtifacts();
    verify(artifactService).get(eq("u1"));
    verify(artifactService).get(eq("u2"));
    verify(artifactService).get(eq("u3"));
    verify(artifactService).get(eq("u4"));
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void shouldFetchRequiredApp() {
    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    doReturn(ContextElementType.STANDARD).when(mockParams).getElementType();
    doReturn(anApplication().appId(APP_ID).build()).when(mockParams).fetchRequiredApp();
    ExecutionContextImpl context = new ExecutionContextImpl(new StateExecutionInstance());
    context.pushContextElement(mockParams);
    assertThat(context.fetchRequiredApp()).isNotNull();
    ExecutionContextImpl context2 = new ExecutionContextImpl(new StateExecutionInstance());
    assertThatThrownBy(context2::fetchRequiredApp).isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void shouldFetchRequiredEnv() {
    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    doReturn(ContextElementType.STANDARD).when(mockParams).getElementType();
    doReturn(anEnvironment().uuid(ENV_ID).build()).when(mockParams).fetchRequiredEnv();
    ExecutionContextImpl context = new ExecutionContextImpl(new StateExecutionInstance());
    context.pushContextElement(mockParams);
    assertThat(context.fetchRequiredEnvironment()).isNotNull();
    ExecutionContextImpl context2 = new ExecutionContextImpl(new StateExecutionInstance());
    assertThatThrownBy(context2::fetchRequiredEnvironment).isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldFetchNoArtifactsForService() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());
    ExecutionContextImpl context = new ExecutionContextImpl(new StateExecutionInstance());
    injector.injectMembers(context);

    Application app = anApplication().name("AppA").accountId(ACCOUNT_ID).build();
    app = appService.save(app);

    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(app.getUuid());
    injector.injectMembers(std);
    context.pushContextElement(std);

    assertThat(context.getArtifactForService(SERVICE_ID)).isNull();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFetchArtifactForServiceFromSweepingOutput() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());
    doReturn(asList(ServiceArtifactElements.builder()
                        .artifactElements(singletonList(
                            ServiceArtifactElement.builder().uuid("u1").serviceIds(asList("s1", "s2")).build()))
                        .build(),
                 ServiceArtifactElements.builder()
                     .artifactElements(singletonList(
                         ServiceArtifactElement.builder().uuid("u2").serviceIds(singletonList("s3")).build()))
                     .build()))
        .when(sweepingOutputService)
        .findSweepingOutputsWithNamePrefix(any(SweepingOutputInquiry.class), eq(Scope.PIPELINE));
    ExecutionContextImpl context = new ExecutionContextImpl(new StateExecutionInstance());
    injector.injectMembers(context);
    Reflect.on(context).set("sweepingOutputService", sweepingOutputService);
    Reflect.on(context).set("artifactService", artifactService);

    Application app = anApplication().name("AppA").accountId(ACCOUNT_ID).build();
    app = appService.save(app);

    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(app.getUuid());
    injector.injectMembers(std);
    context.pushContextElement(std);

    context.getArtifactForService("s2");
    verify(artifactService, times(1)).get(eq("u1"));
    verify(artifactService, never()).get(eq("u2"));

    context.getArtifactForService("s3");
    verify(artifactService, times(1)).get(eq("u1"));
    verify(artifactService, times(1)).get(eq("u2"));
  }

  private ExecutionContextImpl prepareContext(StateExecutionInstance stateExecutionInstance) {
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    injector.injectMembers(context);

    ServiceElement svc = ServiceElement.builder().uuid(generateUuid()).name("svc1").build();
    context.pushContextElement(svc);

    ServiceTemplateElement st = new ServiceTemplateElement();
    st.setUuid(generateUuid());
    st.setName("st1");
    st.setServiceElement(ServiceElement.builder().uuid(generateUuid()).name("svc2").build());
    context.pushContextElement(st);

    HostElement host = HostElement.builder().build();
    host.setUuid(generateUuid());
    host.setHostName("host1");
    context.pushContextElement(host);

    Application app = anApplication().name("AppA").accountId(ACCOUNT_ID).build();
    app = appService.save(app);

    Environment env = anEnvironment().appId(app.getUuid()).name("DEV").build();
    env = environmentService.save(env);

    WorkflowStandardParams std = new WorkflowStandardParams();
    injector.injectMembers(std);

    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());
    std.setArtifactIds(asList(ARTIFACT_ID));
    context.pushContextElement(std);

    return context;
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void shouldRenderExpression() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setDisplayName("abc");
    ExecutionContextImpl context = prepareContext(stateExecutionInstance);
    WorkflowStandardParams std = context.getContextElement(ContextElementType.STANDARD);

    String timeStampId = std.getTimestampId();

    String expr =
        "$HOME/${env.name}/${app.name}/${service.name}/${serviceTemplate.name}/${host.name}/${timestampId}/runtime";
    String path = context.renderExpression(expr);
    assertThat(path).isEqualTo("$HOME/DEV/AppA/svc2/st1/host1/" + timeStampId + "/runtime");
  }

  private void programServiceTemplateService(ExecutionContextImpl context, Artifact artifact) {
    WorkflowStandardParams std = context.getContextElement(ContextElementType.STANDARD);
    ServiceTemplateElement st = context.getContextElement(ContextElementType.SERVICE_TEMPLATE);
    ServiceElement svc = context.getContextElement(ContextElementType.SERVICE);

    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .serviceId(svc.getUuid())
                                          .type(Type.TEXT)
                                          .name("REV")
                                          .value("${artifact.buildNo}".toCharArray())
                                          .build();
    when(serviceTemplateService.computeServiceVariables(context.getAppId(), context.getEnv().getUuid(), st.getUuid(),
             context.getWorkflowExecutionId(), OBTAIN_META))
        .thenReturn(asList(serviceVariable));
    when(serviceTemplateService.getTemplateRefKeysByService(
             context.getAppId(), svc.getUuid(), context.getEnv().getUuid()))
        .thenReturn(asList(new Key(ServiceTemplate.class, "serviceTemplates", st.getUuid())));
    when(artifactService.get(ARTIFACT_ID)).thenReturn(artifact);
    when(serviceResourceService.get(svc.getUuid()))
        .thenReturn(Service.builder().artifactStreamIds(singletonList(ARTIFACT_STREAM_ID)).build());
    when(artifactStreamServiceBindingService.listArtifactStreamIds(svc.getUuid()))
        .thenReturn(singletonList(ARTIFACT_STREAM_ID));
    on(std).set("artifactService", artifactService);
    on(std).set("serviceTemplateService", serviceTemplateService);
    on(std).set("artifactStreamServiceBindingService", artifactStreamServiceBindingService);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldRenderExpressionList() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    ExecutionContextImpl context = prepareContextRenderExpressionList("listExpr", "x, y, z");
    List<String> exprList = ImmutableList.of("a", "b", "${serviceVariable.listExpr}", "c");
    List<String> rendered = context.renderExpressionList(exprList);
    ImmutableList<String> expected = ImmutableList.of("a", "b", "x", "y", "z", "c");
    assertThat(rendered).isEqualTo(expected);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldRenderExpressionListNoSeparator() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    ExecutionContextImpl context = prepareContextRenderExpressionList("listExpr", "xyz");
    List<String> exprList = ImmutableList.of("abc", "${serviceVariable.listExpr}", "def");
    List<String> rendered = context.renderExpressionList(exprList);
    ImmutableList<String> expected = ImmutableList.of("abc", "xyz", "def");
    assertThat(rendered).isEqualTo(expected);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldRenderExpressionListNull() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    ExecutionContextImpl context = prepareContextRenderExpressionList("a", "b");
    assertThat(context.renderExpressionList(null)).isNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldRenderExpressionListWithCustomSeparator() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    ExecutionContextImpl context = prepareContextRenderExpressionList("listExpr", "x:y:z");
    List<String> exprList = ImmutableList.of("a", "b", "${serviceVariable.listExpr}", "c");
    List<String> rendered = context.renderExpressionList(exprList, ":");
    ImmutableList<String> expected = ImmutableList.of("a", "b", "x", "y", "z", "c");
    assertThat(rendered).isEqualTo(expected);
  }

  private ExecutionContextImpl prepareContextRenderExpressionList(String svcVarName, String svcVarValue) {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setExecutionUuid(generateUuid());
    stateExecutionInstance.setDisplayName("http");

    ExecutionContextImpl context = prepareContext(stateExecutionInstance);
    ServiceElement svc = context.getContextElement(ContextElementType.SERVICE);

    on(context).set("serviceTemplateService", serviceTemplateService);

    final PhaseElement phaseElement = PhaseElement.builder().serviceElement(svc).build();
    injector.injectMembers(phaseElement);
    context.pushContextElement(phaseElement);

    WorkflowStandardParams std = context.getContextElement(ContextElementType.STANDARD);
    ServiceTemplateElement st = context.getContextElement(ContextElementType.SERVICE_TEMPLATE);
    ServiceElement svc1 = context.getContextElement(ContextElementType.SERVICE);

    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .serviceId(svc1.getUuid())
                                          .type(Type.TEXT)
                                          .name(svcVarName)
                                          .value(svcVarValue.toCharArray())
                                          .build();
    when(serviceTemplateService.computeServiceVariables(context.getAppId(), context.getEnv().getUuid(), st.getUuid(),
             context.getWorkflowExecutionId(), OBTAIN_META))
        .thenReturn(asList(serviceVariable));
    when(serviceTemplateService.getTemplateRefKeysByService(
             context.getAppId(), svc1.getUuid(), context.getEnv().getUuid()))
        .thenReturn(asList(new Key(ServiceTemplate.class, "serviceTemplates", st.getUuid())));
    on(std).set("serviceTemplateService", serviceTemplateService);
    return context;
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldEvaluateIndirectExpression() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setExecutionUuid(generateUuid());
    stateExecutionInstance.setDisplayName("http-TEST");

    ExecutionContextImpl context = prepareContext(stateExecutionInstance);
    ServiceElement svc = context.getContextElement(ContextElementType.SERVICE);

    on(context).set("serviceTemplateService", serviceTemplateService);

    final PhaseElement phaseElement = PhaseElement.builder().serviceElement(svc).build();
    injector.injectMembers(phaseElement);
    context.pushContextElement(phaseElement);

    Artifact artifact = Artifact.Builder.anArtifact()
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(Maps.newHashMap("buildNo", "123-SNAPSHOT"))
                            .build();

    programServiceTemplateService(context, artifact);

    String expr = "${httpResponseBody}.contains(${serviceVariable.REV})";
    HttpStateExecutionData httpStateExecutionData =
        HttpStateExecutionData.builder().httpResponseBody("abcabcabcabcabc-123-SNAPSHOT-23423sadf").build();
    boolean assertion = (boolean) context.evaluateExpression(
        expr, StateExecutionContext.builder().stateExecutionData(httpStateExecutionData).build());
    assertThat(assertion).isTrue();
  }

  /**
   * Should evaluate indirect references
   */
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldRenderTemplateVariableExpression() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setExecutionUuid(generateUuid());
    stateExecutionInstance.setDisplayName("http");
    ExecutionContextImpl context = prepareContext(stateExecutionInstance);

    Artifact artifact = Artifact.Builder.anArtifact().withMetadata(Maps.newHashMap("buildNo", "123-SNAPSHOT")).build();

    programServiceTemplateService(context, artifact);

    CommandStateExecutionData commandStateExecutionData = CommandStateExecutionData.Builder.aCommandStateExecutionData()
                                                              .withTemplateVariable(ImmutableMap.of("MyVar", "MyValue"))
                                                              .build();
    String expr = "echo ${MyVar}-${artifact.buildNo}";

    String evaluatedExpression = context.renderExpression(
        expr, StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).artifact(artifact).build());
    assertThat(evaluatedExpression).isNotEmpty();
    assertThat(evaluatedExpression).isEqualTo("echo MyValue-123-SNAPSHOT");
  }

  private ExecutionContextImpl prepareSweepingExecutionContext(String appId, String pipelineExecutionId,
      String workflowExecutionId, String phaseId, String stateId, String phaseName) {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid(stateId);
    stateExecutionInstance.setExecutionUuid(workflowExecutionId);
    stateExecutionInstance.getContextElements().add(
        aWorkflowStandardParams()
            .withAppId(appId)
            .withWorkflowElement(WorkflowElement.builder().pipelineDeploymentUuid(pipelineExecutionId).build())
            .build());
    stateExecutionInstance.getContextElements().add(
        PhaseElement.builder().appId(appId).uuid(phaseId).phaseName(phaseName).build());
    return new ExecutionContextImpl(stateExecutionInstance);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldPrepareSweepingOutputBuilderForPipelineScope() {
    String appId = generateUuid();
    String pipelineExecutionId = generateUuid();

    ExecutionContextImpl context = prepareSweepingExecutionContext(appId, pipelineExecutionId, null, null, null, null);
    final SweepingOutputInstance sweepingOutputInstance1 = context.prepareSweepingOutputBuilder(Scope.PIPELINE).build();
    assertThat(sweepingOutputInstance1.getPipelineExecutionId()).isEqualTo(pipelineExecutionId);
    assertThat(sweepingOutputInstance1.getAppId()).isEqualTo(appId);

    final SweepingOutputInstance sweepingOutputInstance2 = context.prepareSweepingOutputBuilder(null).build();
    assertThat(sweepingOutputInstance2.getPipelineExecutionId()).isEqualTo(pipelineExecutionId);
    assertThat(sweepingOutputInstance2.getAppId()).isEqualTo(appId);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldPrepareSweepingOutputBuilderForPipelineScopeJustAWorkflow() {
    String appId = generateUuid();
    String workflowExecutionId = generateUuid();

    ExecutionContextImpl context = prepareSweepingExecutionContext(appId, null, workflowExecutionId, null, null, null);
    final SweepingOutputInstance sweepingOutputInstance1 = context.prepareSweepingOutputBuilder(Scope.PIPELINE).build();
    assertThat(sweepingOutputInstance1.getWorkflowExecutionIds()).containsExactly(workflowExecutionId);
    assertThat(sweepingOutputInstance1.getAppId()).isEqualTo(appId);

    final SweepingOutputInstance sweepingOutputInstance2 = context.prepareSweepingOutputBuilder(null).build();
    assertThat(sweepingOutputInstance1.getWorkflowExecutionIds()).containsExactly(workflowExecutionId);
    assertThat(sweepingOutputInstance2.getAppId()).isEqualTo(appId);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldPrepareSweepingOutputBuilderForWorkflowScope() {
    String appId = generateUuid();
    String pipelineExecutionId = generateUuid();
    String workflowExecutionId = generateUuid();

    ExecutionContextImpl context =
        prepareSweepingExecutionContext(appId, pipelineExecutionId, workflowExecutionId, null, null, null);
    final SweepingOutputInstance sweepingOutputInstance1 = context.prepareSweepingOutputBuilder(Scope.WORKFLOW).build();
    assertThat(sweepingOutputInstance1.getPipelineExecutionId()).isNotEqualTo(pipelineExecutionId);
    assertThat(sweepingOutputInstance1.getWorkflowExecutionIds()).containsExactly(workflowExecutionId);
    assertThat(sweepingOutputInstance1.getAppId()).isEqualTo(appId);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldPrepareSweepingOutputBuilderForPhaseScope() {
    String appId = generateUuid();
    String pipelineExecutionId = generateUuid();
    String workflowExecutionId = generateUuid();
    String phaseId = generateUuid();
    String phaseName = "phaseName";

    ExecutionContextImpl context =
        prepareSweepingExecutionContext(appId, pipelineExecutionId, workflowExecutionId, phaseId, null, phaseName);
    final SweepingOutputInstance sweepingOutputInstance1 = context.prepareSweepingOutputBuilder(Scope.PHASE).build();
    assertThat(sweepingOutputInstance1.getPipelineExecutionId()).isNotEqualTo(pipelineExecutionId);
    assertThat(sweepingOutputInstance1.getWorkflowExecutionIds()).hasSize(1);
    assertThat(sweepingOutputInstance1.getWorkflowExecutionIds()).doesNotContain(workflowExecutionId);
    assertThat(sweepingOutputInstance1.getPhaseExecutionId()).isEqualTo(workflowExecutionId + phaseId + phaseName);
    assertThat(sweepingOutputInstance1.getAppId()).isEqualTo(appId);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldPrepareSweepingOutputBuilderForStateScope() {
    String appId = generateUuid();
    String pipelineExecutionId = generateUuid();
    String workflowExecutionId = generateUuid();
    String phaseId = generateUuid();
    String stateId = generateUuid();
    String phaseName = "phaseName";

    ExecutionContextImpl context =
        prepareSweepingExecutionContext(appId, pipelineExecutionId, workflowExecutionId, phaseId, stateId, phaseName);
    final SweepingOutputInstance sweepingOutputInstance1 = context.prepareSweepingOutputBuilder(Scope.STATE).build();
    assertThat(sweepingOutputInstance1.getPipelineExecutionId()).isNotEqualTo(pipelineExecutionId);
    assertThat(sweepingOutputInstance1.getWorkflowExecutionIds()).hasSize(1);
    assertThat(sweepingOutputInstance1.getWorkflowExecutionIds()).doesNotContain(workflowExecutionId);
    assertThat(sweepingOutputInstance1.getPhaseExecutionId()).isNotEqualTo(workflowExecutionId + phaseId + phaseName);
    assertThat(sweepingOutputInstance1.getStateExecutionId()).isEqualTo(stateId);
    assertThat(sweepingOutputInstance1.getAppId()).isEqualTo(appId);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExpressionEvaluationWithStateName() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setDisplayName("ABC-TEST");

    ExecutionContextImpl context = prepareContext(stateExecutionInstance);
    ServiceElement svc = context.getContextElement(ContextElementType.SERVICE);
    on(context).set("serviceTemplateService", serviceTemplateService);

    final PhaseElement phaseElement = PhaseElement.builder().serviceElement(svc).build();
    injector.injectMembers(phaseElement);
    context.pushContextElement(phaseElement);

    Artifact artifact = Artifact.Builder.anArtifact()
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(Maps.newHashMap("buildNo", "123-SNAPSHOT"))
                            .build();
    programServiceTemplateService(context, artifact);

    String expr = "${httpResponseBody}.contains(${serviceVariable.REV})";
    HttpStateExecutionData httpStateExecutionData =
        HttpStateExecutionData.builder().httpResponseBody("ABC-123-SNAPSHOT-23423ABC").build();
    boolean assertion = (boolean) context.evaluateExpression(
        expr, StateExecutionContext.builder().stateExecutionData(httpStateExecutionData).build());
    assertThat(assertion).isTrue();
    // Don't change the display name. This tests the https://harness.atlassian.net/browse/CD-2111
    assertThat(stateExecutionInstance.getDisplayName()).isEqualTo("ABC-TEST");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getAccumulatedInstanceInfoVariables() {
    ExecutionContextImpl context = new ExecutionContextImpl(new StateExecutionInstance());
    testInstanceScalingUp(context);
    testInstanceScalingDown(context);
    testInstanceScalingUpDown(context);
    testInstanceRolling(context);
  }

  private void testInstanceRolling(ExecutionContextImpl context) {
    InstanceInfoVariables instanceInfoVariables;
    List<SweepingOutput> sweepingOutputs;

    sweepingOutputs = new ArrayList<>(asList(instanceInfoVariablesWithNewTag(asList("1", "2", "3"), asList("4"))));
    instanceInfoVariables = context.getAccumulatedInstanceInfoVariables(sweepingOutputs);
    assertThat(instanceInfoVariables.getInstanceElements()).hasSize(4);
    assertThat(instanceInfoVariables.getInstanceDetails()).hasSize(4);
    assertThat(newHostsFromDetails(instanceInfoVariables.getInstanceDetails())).containsExactlyInAnyOrder("host-4");
    assertThat(newHostsFromElement(instanceInfoVariables.getInstanceElements())).containsExactlyInAnyOrder("host-4");

    sweepingOutputs.add(instanceInfoVariablesWithNewTag(asList("1", "2", "4"), asList("3")));
    instanceInfoVariables = context.getAccumulatedInstanceInfoVariables(sweepingOutputs);
    assertThat(instanceInfoVariables.getInstanceElements()).hasSize(4);
    assertThat(instanceInfoVariables.getInstanceDetails()).hasSize(4);
    assertThat(newHostsFromDetails(instanceInfoVariables.getInstanceDetails()))
        .containsExactlyInAnyOrder("host-4", "host-3");
    assertThat(newHostsFromElement(instanceInfoVariables.getInstanceElements()))
        .containsExactlyInAnyOrder("host-4", "host-3");

    sweepingOutputs.add(instanceInfoVariablesWithNewTag(asList("3", "1", "4"), asList("2")));
    instanceInfoVariables = context.getAccumulatedInstanceInfoVariables(sweepingOutputs);
    assertThat(instanceInfoVariables.getInstanceElements()).hasSize(4);
    assertThat(instanceInfoVariables.getInstanceDetails()).hasSize(4);
    assertThat(newHostsFromDetails(instanceInfoVariables.getInstanceDetails()))
        .containsExactlyInAnyOrder("host-4", "host-3", "host-2");
    assertThat(newHostsFromElement(instanceInfoVariables.getInstanceElements()))
        .containsExactlyInAnyOrder("host-4", "host-3", "host-2");

    sweepingOutputs.add(instanceInfoVariablesWithNewTag(asList("3", "2", "4"), asList("1")));
    instanceInfoVariables = context.getAccumulatedInstanceInfoVariables(sweepingOutputs);
    assertThat(instanceInfoVariables.getInstanceElements()).hasSize(4);
    assertThat(instanceInfoVariables.getInstanceDetails()).hasSize(4);
    assertThat(newHostsFromDetails(instanceInfoVariables.getInstanceDetails()))
        .containsExactlyInAnyOrder("host-4", "host-3", "host-2", "host-1");
    assertThat(newHostsFromElement(instanceInfoVariables.getInstanceElements()))
        .containsExactlyInAnyOrder("host-4", "host-3", "host-2", "host-1");
  }

  private void testInstanceScalingUpDown(ExecutionContextImpl context) {
    InstanceInfoVariables instanceInfoVariables;
    List<SweepingOutput> sweepingOutputs;

    sweepingOutputs = new ArrayList<>(asList(instanceInfoVariablesWithNewTag(asList("1", "2", "3"), asList("4", "5"))));

    instanceInfoVariables = context.getAccumulatedInstanceInfoVariables(sweepingOutputs);
    assertThat(instanceInfoVariables.getInstanceElements()).hasSize(5);
    assertThat(instanceInfoVariables.getInstanceDetails()).hasSize(5);
    assertThat(newHostsFromDetails(instanceInfoVariables.getInstanceDetails()))
        .containsExactlyInAnyOrder("host-4", "host-5");
    assertThat(newHostsFromElement(instanceInfoVariables.getInstanceElements()))
        .containsExactlyInAnyOrder("host-4", "host-5");

    sweepingOutputs.add(instanceInfoVariablesWithNewTag(asList("1", "2", "3", "4", "5"), asList("6", "7")));
    instanceInfoVariables = context.getAccumulatedInstanceInfoVariables(sweepingOutputs);
    assertThat(instanceInfoVariables.getInstanceElements()).hasSize(7);
    assertThat(instanceInfoVariables.getInstanceDetails()).hasSize(7);

    sweepingOutputs.add(instanceInfoVariablesWithNewTag(asList("1", "2", "3", "4", "7"), emptyList()));
    instanceInfoVariables = context.getAccumulatedInstanceInfoVariables(sweepingOutputs);
    assertThat(instanceInfoVariables.getInstanceElements()).hasSize(5);
    assertThat(instanceInfoVariables.getInstanceDetails()).hasSize(5);
    assertThat(newHostsFromDetails(instanceInfoVariables.getInstanceDetails()))
        .containsExactlyInAnyOrder("host-4", "host-7");
    assertThat(newHostsFromElement(instanceInfoVariables.getInstanceElements()))
        .containsExactlyInAnyOrder("host-4", "host-7");
  }

  private void testInstanceScalingDown(ExecutionContextImpl context) {
    InstanceInfoVariables instanceInfoVariables;
    List<SweepingOutput> sweepingOutputs;
    sweepingOutputs =
        new ArrayList<>(asList(instanceInfoVariablesWithNewTag(asList("1", "2", "3"), asList("4", "5", "6")),
            instanceInfoVariablesWithNewTag(asList("1", "2", "3", "4"), emptyList())));
    instanceInfoVariables = context.getAccumulatedInstanceInfoVariables(sweepingOutputs);
    assertThat(instanceInfoVariables.getInstanceElements()).hasSize(4);
    assertThat(instanceInfoVariables.getInstanceDetails()).hasSize(4);
    assertThat(newHostsFromDetails(instanceInfoVariables.getInstanceDetails())).containsExactlyInAnyOrder("host-4");
    assertThat(newHostsFromElement(instanceInfoVariables.getInstanceElements())).containsExactlyInAnyOrder("host-4");

    // scale up again
    sweepingOutputs.add(instanceInfoVariablesWithNewTag(asList("1", "2", "3", "4"), asList("7", "8", "9")));
    instanceInfoVariables = context.getAccumulatedInstanceInfoVariables(sweepingOutputs);
    assertThat(instanceInfoVariables.getInstanceElements()).hasSize(7);
    assertThat(instanceInfoVariables.getInstanceDetails()).hasSize(7);
    assertThat(newHostsFromDetails(instanceInfoVariables.getInstanceDetails()))
        .containsExactlyInAnyOrder("host-4", "host-7", "host-8", "host-9");
    assertThat(newHostsFromElement(instanceInfoVariables.getInstanceElements()))
        .containsExactlyInAnyOrder("host-4", "host-7", "host-9", "host-8");
  }

  private void testInstanceScalingUp(ExecutionContextImpl context) {
    InstanceInfoVariables instanceInfoVariables;
    List<SweepingOutput> sweepingOutputs;

    sweepingOutputs = new ArrayList<>(asList(instanceInfoVariablesWithNewTag(asList("1", "2", "3"), asList("4", "5"))));

    instanceInfoVariables = context.getAccumulatedInstanceInfoVariables(sweepingOutputs);
    assertThat(instanceInfoVariables.getInstanceElements()).hasSize(5);
    assertThat(instanceInfoVariables.getInstanceDetails()).hasSize(5);
    assertThat(newHostsFromDetails(instanceInfoVariables.getInstanceDetails()))
        .containsExactlyInAnyOrder("host-4", "host-5");
    assertThat(newHostsFromElement(instanceInfoVariables.getInstanceElements()))
        .containsExactlyInAnyOrder("host-4", "host-5");

    sweepingOutputs.add(instanceInfoVariablesWithNewTag(asList("1", "2", "3", "4", "5"), asList("6", "7")));
    instanceInfoVariables = context.getAccumulatedInstanceInfoVariables(sweepingOutputs);
    assertThat(instanceInfoVariables.getInstanceElements()).hasSize(7);
    assertThat(instanceInfoVariables.getInstanceDetails()).hasSize(7);
    assertThat(newHostsFromDetails(instanceInfoVariables.getInstanceDetails()))
        .containsExactlyInAnyOrder("host-4", "host-5", "host-6", "host-7");
    assertThat(newHostsFromElement(instanceInfoVariables.getInstanceElements()))
        .containsExactlyInAnyOrder("host-4", "host-5", "host-6", "host-7");

    sweepingOutputs.add(
        instanceInfoVariablesWithNewTag(asList("1", "2", "3", "4", "5", "6", "7"), asList("8", "9", "10")));
    instanceInfoVariables = context.getAccumulatedInstanceInfoVariables(sweepingOutputs);
    assertThat(instanceInfoVariables.getInstanceElements()).hasSize(10);
    assertThat(instanceInfoVariables.getInstanceDetails()).hasSize(10);
    assertThat(newHostsFromDetails(instanceInfoVariables.getInstanceDetails()))
        .containsExactlyInAnyOrder("host-4", "host-5", "host-6", "host-7", "host-8", "host-9", "host-10");
    assertThat(newHostsFromElement(instanceInfoVariables.getInstanceElements()))
        .containsExactlyInAnyOrder("host-4", "host-5", "host-6", "host-7", "host-8", "host-9", "host-10");
  }

  private InstanceInfoVariables instanceInfoVariablesWithNewTag(List<String> oldHosts, List<String> newHosts) {
    return InstanceInfoVariables.builder()
        .instanceDetails(
            Stream.concat(oldHosts.stream(), newHosts.stream())
                .map(name
                    -> InstanceDetails.builder().hostName("host-" + name).newInstance(newHosts.contains(name)).build())
                .collect(Collectors.toList()))
        .instanceElements(
            Stream.concat(oldHosts.stream(), newHosts.stream())
                .map(name -> anInstanceElement().hostName("host-" + name).newInstance(newHosts.contains(name)).build())
                .collect(Collectors.toList()))
        .build();
  }

  private List<String> newHostsFromDetails(List<InstanceDetails> instanceDetails) {
    return instanceDetails.stream()
        .filter(InstanceDetails::isNewInstance)
        .map(InstanceDetails::getHostName)
        .collect(Collectors.toList());
  }

  private List<String> newHostsFromElement(List<InstanceElement> instanceElements) {
    return instanceElements.stream()
        .filter(InstanceElement::isNewInstance)
        .map(InstanceElement::getHostName)
        .collect(Collectors.toList());
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testAppendStateExecutionId() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid("bar");
    ExecutionContext context = new ExecutionContextImpl(stateExecutionInstance);
    String result = context.appendStateExecutionId("foo");
    assertThat(result).isEqualTo("foobar");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testRenderExpressionsForInstanceDetails() {
    ExecutionContextImpl context = Mockito.spy(new ExecutionContextImpl(new StateExecutionInstance()));
    Reflect.on(context).set("sweepingOutputService", sweepingOutputService);
    List<String> expected = asList("host-1", "host-2");
    List<SweepingOutput> sweepingOutputs = asList(InstanceInfoVariables.builder().build());

    doReturn(expected).when(context).renderExpressionFromInstanceInfoVariables(
        anyString(), eq(true), any(InstanceInfoVariables.class));
    doReturn(sweepingOutputs)
        .when(sweepingOutputService)
        .findManyWithNamePrefix(any(SweepingOutputInquiry.class), eq(Scope.PHASE));
    doReturn(sweepingOutputs.get(0)).when(context).getAccumulatedInstanceInfoVariables(anyList());

    assertThat(context.renderExpressionsForInstanceDetails("${instanceDetails.k8s.ip}", true)).isEqualTo(expected);

    verify(sweepingOutputService, times(1))
        .findSweepingOutputsWithNamePrefix(any(SweepingOutputInquiry.class), eq(Scope.PHASE));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testRenderExpressionsForInstanceDetailsForWorkflow() {
    ExecutionContextImpl context = Mockito.spy(new ExecutionContextImpl(new StateExecutionInstance()));
    Reflect.on(context).set("sweepingOutputService", sweepingOutputService);
    List<String> expected = asList("host-1", "host-2");
    List<SweepingOutput> sweepingOutputs = asList(InstanceInfoVariables.builder().build());

    doReturn(expected).when(context).renderExpressionFromInstanceInfoVariables(
        anyString(), eq(true), any(InstanceInfoVariables.class));
    doReturn(sweepingOutputs)
        .when(sweepingOutputService)
        .findManyWithNamePrefix(any(SweepingOutputInquiry.class), eq(Scope.WORKFLOW));
    doReturn(sweepingOutputs.get(0)).when(context).getAccumulatedInstanceInfoVariables(anyList());

    assertThat(context.renderExpressionsForInstanceDetailsForWorkflow("${instanceDetails.k8s.ip}", true))
        .isEqualTo(expected);

    verify(sweepingOutputService, times(1))
        .findSweepingOutputsWithNamePrefix(any(SweepingOutputInquiry.class), eq(Scope.WORKFLOW));
  }
}
