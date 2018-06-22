package software.wings.sm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.common.Constants.ARTIFACT_FILE_NAME_VARIABLE;
import static software.wings.common.Constants.PHASE_PARAM;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.client.utils.URIBuilder;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.api.WorkflowElement;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Environment;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.artifact.Artifact;
import software.wings.common.InstanceExpressionProcessor;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceTemplateService;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Class WorkflowStandardParams.
 *
 * @author Rishi.
 */
public class WorkflowStandardParams implements ExecutionContextAware, ContextElement {
  private static final String STANDARD_PARAMS = "STANDARD_PARAMS";

  @Inject private transient AppService appService;

  @Inject private transient ArtifactService artifactService;

  @Inject private transient EnvironmentService environmentService;

  @Inject private transient ServiceTemplateService serviceTemplateService;

  @Inject private transient MainConfiguration configuration;

  @Inject private transient ArtifactStreamService artifactStreamService;

  private String appId;
  private String envId;
  private List<String> artifactIds;
  private WorkflowElement workflowElement;

  // TODO: centralized in-memory executionCredential and special encrypted mapping
  private ExecutionCredential executionCredential;

  @JsonIgnore @Transient private transient Application app;
  @JsonIgnore @Transient private transient Environment env;
  @JsonIgnore @Transient private transient List<Artifact> artifacts;

  private List<ServiceElement> services;

  private ErrorStrategy errorStrategy;

  private Long startTs;
  private Long endTs;

  private String timestampId = System.currentTimeMillis() + "-" + nextInt(0, 1000);

  @Transient @JsonIgnore private transient ExecutionContext context;

  private Map<String, String> workflowVariables;

  private boolean excludeHostsWithSameArtifact;

  @JsonIgnore private EmbeddedUser currentUser;

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    if (workflowElement != null) {
      map.put(WORKFLOW, workflowElement);
    }
    map.put(APP, getApp());
    map.put(ENV, getEnv());
    map.put(TIMESTAMP_ID, timestampId);

    map.put(DEPLOYMENT_URL,
        buildAbsoluteUrl(format("/account/%s/app/%s/env/%s/executions/%s/details", app.getAccountId(), app.getUuid(),
            BUILD.equals(context.getOrchestrationWorkflowType()) ? "build" : env.getUuid(),
            context.getWorkflowExecutionId())));

    ServiceElement serviceElement = fetchServiceElement(context);
    Artifact artifact;
    if (serviceElement == null) {
      if (isNotEmpty(artifactIds)) {
        artifact = artifactService.get(appId, artifactIds.get(0));
        addArtifactToContext(map, artifact);
      }
    } else {
      artifact = getArtifactForService(serviceElement.getUuid());
      addArtifactToContext(map, artifact);

      List<Key<ServiceTemplate>> templateRefKeysByService =
          serviceTemplateService.getTemplateRefKeysByService(appId, serviceElement.getUuid(), envId);
      if (isEmpty(templateRefKeysByService) || templateRefKeysByService.get(0).getId() == null) {
        map.put(SERVICE_VARIABLE, new HashMap<>());
        map.put(SAFE_DISPLAY_SERVICE_VARIABLE, new HashMap<>());
        return map;
      }
      String templateId = (String) templateRefKeysByService.get(0).getId();

      List<ServiceVariable> serviceVariables = serviceTemplateService.computeServiceVariables(
          appId, envId, templateId, context.getWorkflowExecutionId(), false);
      if (isEmpty(serviceVariables)) {
        map.put(SERVICE_VARIABLE, new HashMap<>());
        map.put(SAFE_DISPLAY_SERVICE_VARIABLE, new HashMap<>());
        return map;
      }

      map.put(SERVICE_VARIABLE,
          serviceVariables.stream().collect(
              Collectors.toMap(ServiceVariable::getName, var -> new String(var.getValue()))));

      map.put(SAFE_DISPLAY_SERVICE_VARIABLE,
          serviceTemplateService
              .computeServiceVariables(appId, envId, templateId, context.getWorkflowExecutionId(), true)
              .stream()
              .collect(Collectors.toMap(ServiceVariable::getName, var -> new String(var.getValue()))));
    }

    return map;
  }

  private void addArtifactToContext(Map<String, Object> map, Artifact artifact) {
    if (artifact != null) {
      artifact.setSource(artifactStreamService.fetchArtifactSourceProperties(
          getApp().getAccountId(), artifact.getAppId(), artifact.getArtifactStreamId()));
      map.put(ARTIFACT, artifact);
      String artifactFileName = null;
      if (isNotEmpty(artifact.getArtifactFiles())) {
        artifactFileName = artifact.getArtifactFiles().get(0).getName();
      } else if (artifact.getMetadata() != null) {
        artifactFileName = artifact.getArtifactFileName();
      }
      if (isNotEmpty(artifactFileName)) {
        map.put(ARTIFACT_FILE_NAME_VARIABLE, artifactFileName);
      }
    }
  }

  private String buildAbsoluteUrl(String fragment) {
    String baseUrl = configuration.getPortal().getUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    try {
      URIBuilder uriBuilder = new URIBuilder(baseUrl);
      uriBuilder.setFragment(fragment);
      return uriBuilder.toString();
    } catch (URISyntaxException e) {
      return baseUrl;
    }
  }

  private ServiceElement fetchServiceElement(ExecutionContext context) {
    ServiceElement serviceElement = context.getContextElement(ContextElementType.SERVICE);
    if (serviceElement != null) {
      return serviceElement;
    }

    ServiceTemplateElement serviceTemplateElement = context.getContextElement(ContextElementType.SERVICE_TEMPLATE);
    if (serviceTemplateElement != null) {
      return serviceTemplateElement.getServiceElement();
    }

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    if (phaseElement != null) {
      return phaseElement.getServiceElement();
    }
    return null;
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public ContextElementType getElementType() {
    return ContextElementType.STANDARD;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return STANDARD_PARAMS;
  }

  /**
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets artifact ids.
   *
   * @return the artifact ids
   */
  public List<String> getArtifactIds() {
    return artifactIds;
  }

  /**
   * Sets artifact ids.
   *
   * @param artifactIds the artifact ids
   */
  public void setArtifactIds(List<String> artifactIds) {
    this.artifactIds = artifactIds;
  }

  /**
   * Gets start ts.
   *
   * @return the start ts
   */
  public Long getStartTs() {
    return startTs;
  }

  /**
   * Sets start ts.
   *
   * @param startTs the start ts
   */
  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  /**
   * Gets end ts.
   *
   * @return the end ts
   */
  public Long getEndTs() {
    return endTs;
  }

  /**
   * Sets end ts.
   *
   * @param endTs the end ts
   */
  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  /**
   * Gets timestamp id.
   *
   * @return the timestamp id
   */
  public String getTimestampId() {
    return timestampId;
  }

  /**
   * Sets timestamp id.
   *
   * @param timestampId the timestamp id
   */
  public void setTimestampId(String timestampId) {
    this.timestampId = timestampId;
  }

  /**
   * Gets execution credential.
   *
   * @return the execution credential
   */
  public ExecutionCredential getExecutionCredential() {
    return executionCredential;
  }

  /**
   * Sets execution credential.
   *
   * @param executionCredential the execution credential
   */
  public void setExecutionCredential(ExecutionCredential executionCredential) {
    this.executionCredential = executionCredential;
  }

  /**
   * Gets services.
   *
   * @return the services
   */
  public List<ServiceElement> getServices() {
    return services;
  }

  /**
   * Sets services.
   *
   * @param services the services
   */
  public void setServices(List<ServiceElement> services) {
    this.services = services;
  }

  /**
   * Gets error strategy.
   *
   * @return the error strategy
   */
  public ErrorStrategy getErrorStrategy() {
    return errorStrategy;
  }

  /**
   * Sets error strategy.
   *
   * @param errorStrategy the error strategy
   */
  public void setErrorStrategy(ErrorStrategy errorStrategy) {
    this.errorStrategy = errorStrategy;
  }

  /**
   * Gets workflow element.
   *
   * @return the workflow element
   */
  public WorkflowElement getWorkflowElement() {
    return workflowElement;
  }

  /**
   * Sets workflow element.
   *
   * @param workflowElement the workflow element
   */
  public void setWorkflowElement(WorkflowElement workflowElement) {
    this.workflowElement = workflowElement;
  }

  public Map<String, String> getWorkflowVariables() {
    return workflowVariables;
  }

  public void setWorkflowVariables(Map<String, String> workflowVariables) {
    this.workflowVariables = workflowVariables;
  }

  public EmbeddedUser getCurrentUser() {
    return currentUser;
  }

  public void setCurrentUser(EmbeddedUser currentUser) {
    this.currentUser = currentUser;
  }

  /**
   * Gets app.
   *
   * @return the app
   */
  public Application getApp() {
    if (app == null && appId != null) {
      app = appService.getApplicationWithDefaults(appId);
    }
    return app;
  }

  /**
   * Gets env.
   *
   * @return the env
   */
  public Environment getEnv() {
    if (env == null && envId != null) {
      env = environmentService.get(appId, envId, false);
    }
    return env;
  }

  /**
   * Gets artifacts.
   *
   * @return the artifacts
   */
  public List<Artifact> getArtifacts() {
    if (artifacts == null && isNotEmpty(artifactIds)) {
      List<Artifact> list = new ArrayList<>();
      for (String artifactId : artifactIds) {
        Artifact artifact = artifactService.get(appId, artifactId);
        if (artifact != null) {
          list.add(artifact);
        }
      }
      artifacts = list;
    }
    return artifacts;
  }

  /**
   * Gets artifact for service.
   *
   * @param serviceId the service id
   * @return the artifact for service
   */
  public Artifact getArtifactForService(String serviceId) {
    getArtifacts();
    if (isEmpty(artifacts)) {
      return null;
    }
    return artifacts.stream().filter(artifact -> artifact.getServiceIds().contains(serviceId)).findFirst().orElse(null);
  }

  @Override
  public String getUuid() {
    return null;
  }

  /**
   * Sets uuid.
   *
   * @param uuid the uuid
   */
  public void setUuid(String uuid) {}

  public List<InstanceElement> getInstances() {
    return (List<InstanceElement>) context.evaluateExpression(InstanceExpressionProcessor.DEFAULT_EXPRESSION);
  }

  protected ExecutionContext getContext() {
    return context;
  }

  @Override
  public void setExecutionContext(ExecutionContext executionContext) {
    this.context = executionContext;
  }

  public boolean isExcludeHostsWithSameArtifact() {
    return excludeHostsWithSameArtifact;
  }

  public void setExcludeHostsWithSameArtifact(boolean excludeHostsWithSameArtifact) {
    this.excludeHostsWithSameArtifact = excludeHostsWithSameArtifact;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String appId;
    private String envId;
    private List<String> artifactIds;
    // TODO: centralized in-memory executionCredential and special encrypted mapping
    private ExecutionCredential executionCredential;
    private List<ServiceElement> services;
    private Long startTs;
    private Long endTs;
    private String timestampId = System.currentTimeMillis() + "-" + nextInt(0, 1000);
    private EmbeddedUser currentUser;
    private boolean excludeHostsWithSameArtifact;
    private WorkflowElement workflowElement;

    private Builder() {}

    /**
     * A workflow standard params builder.
     *
     * @return the builder
     */
    public static Builder aWorkflowStandardParams() {
      return new Builder();
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With artifact ids builder.
     *
     * @param artifactIds the artifact ids
     * @return the builder
     */
    public Builder withArtifactIds(List<String> artifactIds) {
      this.artifactIds = artifactIds;
      return this;
    }

    /**
     * With execution credential builder.
     *
     * @param executionCredential the execution credential
     * @return the builder
     */
    public Builder withExecutionCredential(ExecutionCredential executionCredential) {
      this.executionCredential = executionCredential;
      return this;
    }

    /**
     * With services builder.
     *
     * @param services the services
     * @return the builder
     */
    public Builder withServices(List<ServiceElement> services) {
      this.services = services;
      return this;
    }

    /**
     * With start ts builder.
     *
     * @param startTs the start ts
     * @return the builder
     */
    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts builder.
     *
     * @param endTs the end ts
     * @return the builder
     */
    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /***
     * With WokflowElement builder
     */
    public Builder withWorkflowElement(WorkflowElement workflowElement) {
      this.workflowElement = workflowElement;
      return this;
    }

    /**
     * Build workflow standard params.
     *
     * @return the workflow standard params
     */
    @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
    public WorkflowStandardParams build() {
      WorkflowStandardParams workflowStandardParams = new WorkflowStandardParams();
      workflowStandardParams.setAppId(appId);
      workflowStandardParams.setEnvId(envId);
      workflowStandardParams.setArtifactIds(artifactIds);
      workflowStandardParams.setExecutionCredential(executionCredential);
      workflowStandardParams.setServices(services);
      workflowStandardParams.setStartTs(startTs);
      workflowStandardParams.setEndTs(endTs);
      workflowStandardParams.setTimestampId(timestampId);
      workflowStandardParams.setCurrentUser(currentUser);
      workflowStandardParams.setExcludeHostsWithSameArtifact(excludeHostsWithSameArtifact);
      workflowStandardParams.setWorkflowElement(workflowElement);
      return workflowStandardParams;
    }
  }
}
