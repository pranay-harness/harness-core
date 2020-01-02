package software.wings.sm;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.limits.LimitCheckerFactory;
import io.harness.rule.OwnerRule.Owner;
import org.assertj.core.util.Maps;
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
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.api.WorkflowElement;
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
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

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
  public void shouldfetchNoArtifacts() {
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
  public void shouldfetchNoArtifactsForService() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());
    ExecutionContextImpl context = new ExecutionContextImpl(new StateExecutionInstance());
    injector.injectMembers(context);

    Application app = anApplication().name("AppA").accountId(ACCOUNT_ID).build();
    app = appService.save(app);

    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(app.getUuid());
    std.setArtifactIds(asList(ARTIFACT_ID));
    injector.injectMembers(std);
    context.pushContextElement(std);

    assertThat(context.getArtifactForService(SERVICE_ID)).isNull();
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

    HostElement host = new HostElement();
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
}
