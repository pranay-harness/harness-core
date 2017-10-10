package software.wings.service.intfc;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Pipeline;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import javax.validation.Valid;

/**
 * Created by anubhaw on 10/26/16.
 */
public interface PipelineService {
  /**
   * List pipelines page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Pipeline> listPipelines(PageRequest<Pipeline> pageRequest);

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
   * Create pipeline pipeline.
   *
   * @param pipeline the pipeline
   * @return the pipeline
   */
  @ValidationGroups(Create.class) Pipeline createPipeline(@Valid Pipeline pipeline);

  /**
   * Update pipeline pipeline.
   *
   * @param pipeline the pipeline
   * @return the pipeline
   */
  @ValidationGroups(Update.class) Pipeline updatePipeline(@Valid Pipeline pipeline);

  /**
   * Delete pipeline boolean.
   *
   * @param appId      the app id
   * @param pipelineId the pipeline id
   * @return the boolean
   */
  boolean deletePipeline(String appId, String pipelineId);

  /**
   * Delete pipeline by application
   * @param appId
   * @return
   */
  boolean deletePipelineByApplication(String appId);

  /**
   * Clone pipeline pipeline.
   *
   * @param originalPipelineId the original pipeline id
   * @param pipeline           the pipeline
   * @return the pipeline
   */
  Pipeline clonePipeline(String originalPipelineId, Pipeline pipeline);
}
