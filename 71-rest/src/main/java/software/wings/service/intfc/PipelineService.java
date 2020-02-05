package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.EntityType;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.service.intfc.ownership.OwnedByApplication;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

/**
 * Created by anubhaw on 10/26/16.
 */
public interface PipelineService extends OwnedByApplication {
  /**
   * List pipelines page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Pipeline> listPipelines(PageRequest<Pipeline> pageRequest);

  PageResponse<Pipeline> listPipelines(PageRequest<Pipeline> pageRequest, boolean withDetails,
      Integer previousExecutionsCount, boolean withTags, String tagFilter);

  /**
   * Read pipeline pipeline.
   *
   * @param appId        the app id
   * @param pipelineId   the pipeline id
   * @param withServices the with services
   * @return the pipeline
   */
  Pipeline readPipeline(String appId, String pipelineId, boolean withServices);

  boolean pipelineExists(@NotEmpty String appId, @NotEmpty String pipelineId);
  /**
   * Read pipeline with variables.
   *
   * @param appId        the app id
   * @param pipelineId   the pipeline id
   * @return the pipeline
   */
  Pipeline readPipelineWithVariables(String appId, String pipelineId);

  /**
   * Reads pipeline with Services, EnvironmentIds, Pipeline Variables and Resolved Pipeline variables
   *
   * @return
   */
  Pipeline readPipelineWithResolvedVariables(String appId, String pipelineId, Map<String, String> workflowVariables);

  Pipeline readPipelineWithResolvedVariables(
      String appId, String pipelineId, Map<String, String> pipelineVariables, Map<String, Workflow> workflowCache);

  Pipeline getPipelineByName(String appId, String pipelineName);

  void setPipelineDetails(List<Pipeline> pipelines, boolean withFinalValuesOnly);

  /**
   * Create pipeline pipeline.
   *
   * @param pipeline the pipeline
   * @return the pipeline
   */
  @ValidationGroups(Create.class) Pipeline save(@Valid Pipeline pipeline);

  /**
   * Update pipeline pipeline.
   *
   * @param pipeline the pipeline
   * @param migration
   * @param fromYaml
   * @return the pipeline
   */
  @ValidationGroups(Update.class) Pipeline update(@Valid Pipeline pipeline, boolean migration, boolean fromYaml);

  /**
   * Update pipeline failure strategies.
   *
   * @param appId the app id
   * @param pipelineId the pipeline id
   * @param failureStrategies the new set of failureStrategies
   * @return the pipeline
   */
  @ValidationGroups(Update.class)
  List<FailureStrategy> updateFailureStrategies(
      String appId, String pipelineId, List<FailureStrategy> failureStrategies);

  /**
   * Delete pipeline boolean.
   *
   * @param appId      the app id
   * @param pipelineId the pipeline id
   * @return the boolean
   */
  boolean deletePipeline(String appId, String pipelineId);

  /**
   * Prune pipeline descending objects.
   *
   * @param appId      the app id
   * @param pipelineId the pipeline id
   */
  void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String pipelineId);

  Pipeline clonePipeline(Pipeline originalPipeline, Pipeline pipeline);

  List<EntityType> getRequiredEntities(String appId, String pipelineId);

  void deleteByYamlGit(String appId, String pipelineId, boolean syncFromGit);

  String fetchPipelineName(@NotEmpty String appId, @NotEmpty String pipelineId);

  /***
   * It verifies if the Templated entityIds like Service Infrastructure and Service referenced in the pipeline template
   * @param appId
   * @param templatedEntityId
   * @return Returns the list of names
   */
  List<String> obtainPipelineNamesReferencedByTemplatedEntity(
      @NotEmpty String appId, @NotEmpty String templatedEntityId);

  /**
   * Check if environment is referenced
   *
   * @param appId app Id
   * @param envId env Id
   * @return List of referenced pipelines
   */
  List<String> obtainPipelineNamesReferencedByEnvironment(String appId, @NotEmpty String envId);

  DeploymentMetadata fetchDeploymentMetadata(String appId, String pipelineId, Map<String, String> pipelineVariables,
      List<String> artifactNeededServiceIds, List<String> envIds, boolean withDefaultArtifact,
      WorkflowExecution workflowExecution, DeploymentMetadata.Include... includeList);

  DeploymentMetadata fetchDeploymentMetadata(String appId, String pipelineId, Map<String, String> pipelineVariables,
      List<String> artifactNeededServiceIds, List<String> envIds, DeploymentMetadata.Include... includeList);

  DeploymentMetadata fetchDeploymentMetadata(String appId, Pipeline pipeline, List<String> artifactNeededServiceIds,
      List<String> envIds, DeploymentMetadata.Include... includeList);
}
