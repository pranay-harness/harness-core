/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.FeatureName;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;

import software.wings.api.InfraMappingElement;
import software.wings.api.InstanceElement;
import software.wings.api.ServiceElement;
import software.wings.api.WorkflowElement;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.beans.artifact.Artifact;
import software.wings.common.InstanceExpressionProcessor;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.applicationmanifest.HelmChartService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.inject.Inject;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.jexl3.JexlException;
import org.apache.http.client.utils.URIBuilder;
import org.mongodb.morphia.annotations.Transient;

/**
 * The Class WorkflowStandardParams.
 *
 * @author Rishi.
 */
@OwnedBy(CDC)
@TargetModule(_957_CG_BEANS)
public class WorkflowStandardParams implements ExecutionContextAware, ContextElement {
  private static final String STANDARD_PARAMS = "STANDARD_PARAMS";
  public static final String DEPLOYMENT_TRIGGERED_BY = "deploymentTriggeredBy";

  @Inject private transient AppService appService;
  @Inject private transient AccountService accountService;

  @Inject private transient ArtifactService artifactService;

  @Inject private transient EnvironmentService environmentService;

  @Inject private transient ServiceTemplateService serviceTemplateService;

  @Inject private transient MainConfiguration configuration;

  @Inject private transient ArtifactStreamService artifactStreamService;

  @Inject private transient BuildSourceService buildSourceService;

  @Inject private transient WorkflowExecutionService workflowExecutionService;

  @Inject private transient ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  @Inject private transient SweepingOutputService sweepingOutputService;

  @Inject private transient FeatureFlagService featureFlagService;
  @Inject private transient SubdomainUrlHelperIntfc subdomainUrlHelper;
  @Inject private transient HelmChartService helmChartService;
  @Inject private transient ApplicationManifestService applicationManifestService;

  private String appId;
  private String envId;
  private List<String> artifactIds;
  private List<String> rollbackArtifactIds;
  private List<String> helmChartIds;
  private WorkflowElement workflowElement;

  // TODO: centralized in-memory executionCredential and special encrypted mapping
  private ExecutionCredential executionCredential;

  @JsonIgnore private transient Application app;
  @JsonIgnore @Transient private transient Environment env;
  @JsonIgnore @Transient private transient List<Artifact> artifacts;
  @JsonIgnore @Transient private transient List<Artifact> rollbackArtifacts;
  @JsonIgnore @Transient private transient List<HelmChart> helmCharts;
  @JsonIgnore @Transient private transient Account account;

  private List<ServiceElement> services;

  private ErrorStrategy errorStrategy;

  private Long startTs;
  private Long endTs;

  private String timestampId = System.currentTimeMillis() + "-" + nextInt(0, 1000);

  @Transient @JsonIgnore private transient ExecutionContext context;

  private Map<String, String> workflowVariables;

  private boolean excludeHostsWithSameArtifact;
  @Getter @Setter private boolean notifyTriggeredUserOnly;
  @Getter @Setter private boolean continueWithDefaultValues;
  @Getter @Setter private List<String> executionHosts;

  @JsonIgnore private EmbeddedUser currentUser;

  @Getter @Setter private String lastDeployPhaseId;
  @Getter @Setter private String lastRollbackPhaseId;

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
      if (WorkflowType.ORCHESTRATION == context.getWorkflowType() && workflowElement.getStartTs() == null) {
        workflowElement.setStartTs(workflowExecutionService.fetchWorkflowExecutionStartTs(
            getApp().getAppId(), context.getWorkflowExecutionId()));
      }
      map.put(WORKFLOW, workflowElement);
    }
    map.put(APP, getApp());
    map.put(ACCOUNT, getAccount());
    map.put(ENV, getEnv());
    map.put(TIMESTAMP_ID, timestampId);

    String envUrlId = null;
    if (env != null) {
      envUrlId = env.getUuid();
    } else {
      envUrlId = BUILD == context.getOrchestrationWorkflowType() ? "build" : "noEnv";
    }

    String url;
    if (workflowElement != null && isNotBlank(workflowElement.getPipelineDeploymentUuid())) {
      url = format("/account/%s/app/%s/pipeline-execution/%s/workflow-execution/%s/details", app.getAccountId(),
          app.getUuid(), workflowElement.getPipelineDeploymentUuid(), context.getWorkflowExecutionId());
    } else {
      url = format("/account/%s/app/%s/env/%s/executions/%s/details", app.getAccountId(), app.getUuid(), envUrlId,
          context.getWorkflowExecutionId());
    }
    map.put(DEPLOYMENT_URL, buildAbsoluteUrl(url, context.getAccountId()));

    if (currentUser != null) {
      map.put(DEPLOYMENT_TRIGGERED_BY, currentUser.getName());
    }

    InfraMappingElement infraMappingElement = context.fetchInfraMappingElement();
    if (infraMappingElement != null) {
      map.put(INFRA, infraMappingElement);
    }

    ServiceElement serviceElement = context.fetchServiceElement();
    if (serviceElement == null) {
      if (isNotEmpty(artifactIds)) {
        Artifact artifact = artifactService.get(artifactIds.get(0));
        ExecutionContextImpl.addArtifactToContext(
            artifactStreamService, getApp().getAccountId(), map, artifact, buildSourceService, false);
      }
      if (isNotEmpty(rollbackArtifactIds)) {
        Artifact rollbackArtifact = artifactService.get(rollbackArtifactIds.get(0));
        ExecutionContextImpl.addArtifactToContext(
            artifactStreamService, getApp().getAccountId(), map, rollbackArtifact, buildSourceService, true);
      }
    } else {
      String accountId = getApp().getAccountId();
      String serviceId = serviceElement.getUuid();
      if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        Artifact artifact = getArtifactForService(serviceId);
        ExecutionContextImpl.addArtifactToContext(
            artifactStreamService, accountId, map, artifact, buildSourceService, false);

        Artifact rollbackArtifact = getRollbackArtifactForService(serviceId);
        ExecutionContextImpl.addArtifactToContext(
            artifactStreamService, getApp().getAccountId(), map, rollbackArtifact, buildSourceService, true);
      }
      if (featureFlagService.isEnabled(FeatureName.HELM_CHART_AS_ARTIFACT, accountId)) {
        HelmChart helmChart = getHelmChartForService(serviceId);
        ExecutionContextImpl.addHelmChartToContext(appId, map, helmChart, applicationManifestService);
      }
    }

    return map;
  }

  public HelmChart getHelmChartForService(String serviceId) {
    getHelmCharts();
    if (isEmpty(helmCharts)) {
      return null;
    }

    return helmCharts.stream().filter(helmChart -> serviceId.equals(helmChart.getServiceId())).findFirst().orElse(null);
  }

  public List<HelmChart> getHelmCharts() {
    if (isEmpty(helmCharts) && isNotEmpty(helmChartIds)) {
      helmCharts = helmChartService.listByIds(getApp().getAccountId(), helmChartIds);
    }
    return helmCharts;
  }

  private String buildAbsoluteUrl(String fragment, String accountId) {
    String baseUrl = subdomainUrlHelper.getPortalBaseUrl(accountId);
    try {
      URIBuilder uriBuilder = new URIBuilder(baseUrl);
      uriBuilder.setFragment(fragment);
      return uriBuilder.toString();
    } catch (URISyntaxException e) {
      return baseUrl;
    }
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
   * Sets rollback artifact ids.
   *
   * @param rollbackArtifactIds the artifact ids
   */
  public void setRollbackArtifactIds(List<String> rollbackArtifactIds) {
    this.rollbackArtifactIds = rollbackArtifactIds;
  }

  /**
   * Gets rollback artifact ids.
   *
   * @return  the rollback artifact ids
   */
  public List<String> getRollbackArtifactIds() {
    return rollbackArtifactIds;
  }

  /**
   * Gets helm chart ids.
   *
   * @return the helm chart ids
   */
  public List<String> getHelmChartIds() {
    return helmChartIds;
  }

  /**
   * Sets artifact ids.
   *
   * @param helmChartIds the helm chart ids
   */
  public void setHelmChartIds(List<String> helmChartIds) {
    this.helmChartIds = helmChartIds;
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

  public Application fetchRequiredApp() {
    Application application = getApp();
    if (application == null) {
      throw new InvalidRequestException("App cannot be null");
    }
    return application;
  }

  private Account getAccount() {
    String accountId = getApp() == null ? null : getApp().getAccountId();
    if (account == null && accountId != null) {
      account = accountService.getAccountWithDefaults(accountId);
    }
    return account;
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

  public Environment fetchRequiredEnv() {
    Environment environment = getEnv();
    if (environment == null) {
      throw new InvalidRequestException("Env cannot be null");
    }
    return environment;
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
        Artifact artifact = artifactService.get(artifactId);
        if (artifact != null) {
          list.add(artifact);
        }
      }
      artifacts = list;
    }
    return artifacts;
  }

  /**
   * Gets rollback artifacts.
   *
   * @return the rollback artifacts
   */
  public List<Artifact> getRollbackArtifacts() {
    if (rollbackArtifacts == null && isNotEmpty(rollbackArtifactIds)) {
      List<Artifact> list = new ArrayList<>();
      for (String rollbackArtifactId : rollbackArtifactIds) {
        Artifact rollbackArtifact = artifactService.get(rollbackArtifactId);
        if (rollbackArtifact != null) {
          list.add(rollbackArtifact);
        }
      }
      rollbackArtifacts = list;
    }
    return rollbackArtifacts;
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

    List<String> artifactStreamIds = artifactStreamServiceBindingService.listArtifactStreamIds(serviceId);
    if (isEmpty(artifactStreamIds)) {
      return null;
    }

    return artifacts.stream()
        .filter(artifact -> artifactStreamIds.contains(artifact.getArtifactStreamId()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets rollback artifact for service.
   *
   * @param serviceId the service id
   * @return the rollback artifact for service
   */
  public Artifact getRollbackArtifactForService(String serviceId) {
    getRollbackArtifacts();
    if (isEmpty(rollbackArtifacts)) {
      return null;
    }

    List<String> rollbackArtifactStreamIds = artifactStreamServiceBindingService.listArtifactStreamIds(serviceId);
    if (isEmpty(rollbackArtifactStreamIds)) {
      return null;
    }

    return rollbackArtifacts.stream()
        .filter(rollbackArtifact -> rollbackArtifactStreamIds.contains(rollbackArtifact.getArtifactStreamId()))
        .findFirst()
        .orElse(null);
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
    try {
      return (List<InstanceElement>) context.evaluateExpression(InstanceExpressionProcessor.DEFAULT_EXPRESSION);
    } catch (JexlException ex) {
      return new ArrayList<InstanceElement>();
    }
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
    private boolean notifyTriggeredUserOnly;
    private List<String> executionHosts;

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

    public Builder withCurrentUser(EmbeddedUser currentUser) {
      this.currentUser = currentUser;
      return this;
    }

    public Builder withExecutionHosts(List<String> executionHosts) {
      this.executionHosts = executionHosts;
      return this;
    }

    /**
     * Build workflow standard params.
     *
     * @return the workflow standard params
     */
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
      workflowStandardParams.setNotifyTriggeredUserOnly(notifyTriggeredUserOnly);
      workflowStandardParams.setWorkflowElement(workflowElement);
      workflowStandardParams.setExecutionHosts(executionHosts);
      return workflowStandardParams;
    }
  }
}
