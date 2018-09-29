package software.wings.sm;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.when;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
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
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.api.WorkflowElement;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SweepingOutput;
import software.wings.beans.SweepingOutput.Scope;
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
  @Inject Injector injector;

  @Inject @InjectMocks AppService appService;
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

  private ExecutionContextImpl prepareContext(StateExecutionInstance stateExecutionInstance) {
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
    injector.injectMembers(std);

    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());
    std.setArtifactIds(asList(ARTIFACT_ID));
    context.pushContextElement(std);

    return context;
  }

  @Test
  public void shouldRenderExpression() {
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
                                          .name("REV")
                                          .value("${artifact.buildNo}".toCharArray())
                                          .build();
    when(serviceTemplateService.computeServiceVariables(
             context.getAppId(), context.getEnv().getUuid(), st.getUuid(), context.getWorkflowExecutionId(), false))
        .thenReturn(asList(serviceVariable));
    when(serviceTemplateService.getTemplateRefKeysByService(
             context.getAppId(), svc.getUuid(), context.getEnv().getUuid()))
        .thenReturn(asList(new Key(ServiceTemplate.class, "serviceTemplates", st.getUuid())));
    when(artifactService.get(context.getAppId(), ARTIFACT_ID)).thenReturn(artifact);
    on(std).set("artifactService", artifactService);
    on(std).set("serviceTemplateService", serviceTemplateService);
  }

  @Test
  public void shouldEvaluateIndirectExpression() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setExecutionUuid(generateUuid());
    stateExecutionInstance.setDisplayName("http");

    ExecutionContextImpl context = prepareContext(stateExecutionInstance);
    ServiceElement svc = context.getContextElement(ContextElementType.SERVICE);

    on(context).set("serviceTemplateService", serviceTemplateService);

    final PhaseElement phaseElement = aPhaseElement().withServiceElement(svc).build();
    injector.injectMembers(phaseElement);
    context.pushContextElement(phaseElement);

    Artifact artifact = Artifact.Builder.anArtifact()
                            .withServiceIds(asList(svc.getUuid()))
                            .withMetadata(Maps.newHashMap("buildNo", "123-SNAPSHOT"))
                            .build();

    programServiceTemplateService(context, artifact);

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
    ExecutionContextImpl context = prepareContext(stateExecutionInstance);
    ServiceElement svc = context.getContextElement(ContextElementType.SERVICE);

    Artifact artifact = Artifact.Builder.anArtifact()
                            .withServiceIds(asList(svc.getUuid()))
                            .withMetadata(Maps.newHashMap("buildNo", "123-SNAPSHOT"))
                            .build();

    programServiceTemplateService(context, artifact);

    CommandStateExecutionData commandStateExecutionData = CommandStateExecutionData.Builder.aCommandStateExecutionData()
                                                              .withTemplateVariable(ImmutableMap.of("MyVar", "MyValue"))
                                                              .build();
    String expr = "echo ${MyVar}-${artifact.buildNo}";

    String evaluatedExpression = context.renderExpression(expr, commandStateExecutionData, artifact);
    assertThat(evaluatedExpression).isNotEmpty();
    assertThat(evaluatedExpression).isEqualTo("echo MyValue-123-SNAPSHOT");
  }

  private ExecutionContextImpl prepareSweepingExecutionContext(
      String appId, String pipelineExecutionId, String workflowExecutionId, String phaseId) {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setExecutionUuid(workflowExecutionId);
    stateExecutionInstance.setPhaseSubWorkflowId(phaseId);
    stateExecutionInstance.getContextElements().add(
        aWorkflowStandardParams()
            .withAppId(appId)
            .withWorkflowElement(WorkflowElement.builder().pipelineDeploymentUuid(pipelineExecutionId).build())
            .build());
    return new ExecutionContextImpl(stateExecutionInstance);
  }

  @Test
  public void shouldPrepareSweepingOutputBuilderForPipelineScope() {
    String appId = generateUuid();
    String pipelineExecutionId = generateUuid();

    ExecutionContextImpl context = prepareSweepingExecutionContext(appId, pipelineExecutionId, null, null);
    final SweepingOutput sweepingOutput1 = context.prepareSweepingOutputBuilder(Scope.PIPELINE).build();
    assertThat(sweepingOutput1.getPipelineExecutionId()).isEqualTo(pipelineExecutionId);
    assertThat(sweepingOutput1.getAppId()).isEqualTo(appId);

    final SweepingOutput sweepingOutput2 = context.prepareSweepingOutputBuilder(null).build();
    assertThat(sweepingOutput2.getPipelineExecutionId()).isEqualTo(pipelineExecutionId);
    assertThat(sweepingOutput2.getAppId()).isEqualTo(appId);
  }

  @Test
  public void shouldPrepareSweepingOutputBuilderForPipelineScopeJustAWorkflow() {
    String appId = generateUuid();
    String workflowExecutionId = generateUuid();

    ExecutionContextImpl context = prepareSweepingExecutionContext(appId, null, workflowExecutionId, null);
    final SweepingOutput sweepingOutput1 = context.prepareSweepingOutputBuilder(Scope.PIPELINE).build();
    assertThat(sweepingOutput1.getWorkflowExecutionId()).isEqualTo(workflowExecutionId);
    assertThat(sweepingOutput1.getAppId()).isEqualTo(appId);

    final SweepingOutput sweepingOutput2 = context.prepareSweepingOutputBuilder(null).build();
    assertThat(sweepingOutput2.getWorkflowExecutionId()).isEqualTo(workflowExecutionId);
    assertThat(sweepingOutput2.getAppId()).isEqualTo(appId);
  }

  @Test
  public void shouldPrepareSweepingOutputBuilderForWorkflowScope() {
    String appId = generateUuid();
    String pipelineExecutionId = generateUuid();
    String workflowExecutionId = generateUuid();

    ExecutionContextImpl context =
        prepareSweepingExecutionContext(appId, pipelineExecutionId, workflowExecutionId, null);
    final SweepingOutput sweepingOutput1 = context.prepareSweepingOutputBuilder(Scope.WORKFLOW).build();
    assertThat(sweepingOutput1.getPipelineExecutionId()).isNotEqualTo(pipelineExecutionId);
    assertThat(sweepingOutput1.getWorkflowExecutionId()).isEqualTo(workflowExecutionId);
    assertThat(sweepingOutput1.getAppId()).isEqualTo(appId);
  }

  @Test
  public void shouldPrepareSweepingOutputBuilderForPhaseScope() {
    String appId = generateUuid();
    String pipelineExecutionId = generateUuid();
    String workflowExecutionId = generateUuid();
    String phaseId = generateUuid();

    ExecutionContextImpl context =
        prepareSweepingExecutionContext(appId, pipelineExecutionId, workflowExecutionId, null);
    final SweepingOutput sweepingOutput1 = context.prepareSweepingOutputBuilder(Scope.PHASE).build();
    assertThat(sweepingOutput1.getPipelineExecutionId()).isNotEqualTo(pipelineExecutionId);
    assertThat(sweepingOutput1.getWorkflowExecutionId()).isNotEqualTo(workflowExecutionId);
    assertThat(sweepingOutput1.getPhaseExecutionId()).isNotEqualTo(workflowExecutionId + phaseId);
    assertThat(sweepingOutput1.getAppId()).isEqualTo(appId);
  }
}
