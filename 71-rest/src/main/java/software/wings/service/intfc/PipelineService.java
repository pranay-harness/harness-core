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

  /**
   * Check if environment is referenced
   *
   * @param appId app Id
   * @param envId env Id
   * @return List of referenced pipelines
   */
  List<String> isEnvironmentReferenced(String appId, @NotEmpty String envId);

  /**
   * List pipelines page response.
   *
   * @param pageRequest the page request
   * @param withDetails with details
   * @return the page response
   */
  PageResponse<Pipeline> listPipelines(
      PageRequest<Pipeline> pageRequest, boolean withDetails, Integer previousExecutionsCount);

  /**
   * Read pipeline pipeline.
   *
   * @param appId        the app id
   * @param pipelineId   the pipeline id
   * @param withServices the with services
   * @return the pipeline
   */
  Pipeline readPipeline(String appId, String pipelineId, boolean withServices);

  /**
   * Reads pipeline with Services, EnvironmentIds, Pipeline Variables and Resolved Pipeline variables
   *
   * @return
   */
  Pipeline readPipelineWithResolvedVariables(String appId, String pipelineId, Map<String, String> workflowVariables);

  Pipeline getPipelineByName(String appId, String pipelineName);

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
   * @return the pipeline
   */
  @ValidationGroups(Update.class) Pipeline update(@Valid Pipeline pipeline);

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

  /**
   * Clone pipeline pipeline.
   *
   * @param originalPipelineId the original pipeline id
   * @param pipeline           the pipeline
   * @return the pipeline
   */
  Pipeline clonePipeline(String originalPipelineId, Pipeline pipeline);

  List<EntityType> getRequiredEntities(String appId, String pipelineId);

  void deleteByYamlGit(String appId, String pipelineId, boolean syncFromGit);

  String fetchPipelineName(@NotEmpty String appId, @NotEmpty String pipelineId);
}
