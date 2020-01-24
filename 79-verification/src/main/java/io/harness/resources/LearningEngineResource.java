package io.harness.resources;

import static software.wings.utils.Misc.parseApisVersion;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.harness.service.LearningEngineError;
import io.harness.service.intfc.LearningEngineService;
import io.swagger.annotations.Api;
import software.wings.common.VerificationConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Resource used by Learning Engine.
 * Created by rsingh on 09/05/17.
 */
@Api(LearningEngineService.RESOURCE_URL)
@Path("/" + LearningEngineService.RESOURCE_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class LearningEngineResource {
  @Inject private LearningEngineService learningEngineService;

  /**
   * API to fetch next available task.
   * @param acceptHeaders
   * @return
   */
  @GET
  @Path("/get-next-cv-task")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @Produces({"application/json", "application/v1+json"})
  public RestResponse<LearningEngineAnalysisTask> getNext24x7Task(@HeaderParam("Accept") String acceptHeaders,
      @QueryParam("is24x7") String is24x7, @QueryParam("taskTypes") List<MLAnalysisType> taskTypes) {
    return new RestResponse<>(learningEngineService.getNextLearningEngineAnalysisTask(
        parseApisVersion(acceptHeaders), Optional.ofNullable(is24x7), Optional.of(taskTypes)));
  }

  /**
   * API to fetch next available experimental task.
   * @param experimentName
   * @param acceptHeaders
   * @return
   */
  @GET
  @Path("/get-next-exp-task")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @Produces({"application/json", "application/v1+json"})
  public RestResponse<LearningEngineExperimentalAnalysisTask> getNextExperimentalTask(
      @HeaderParam("Accept") String acceptHeaders, @QueryParam("experimentName") String experimentName,
      @QueryParam("taskTypes") List<MLAnalysisType> taskTypes) {
    return new RestResponse<>(learningEngineService.getNextLearningEngineExperimentalAnalysisTask(
        parseApisVersion(acceptHeaders), experimentName, Optional.of(taskTypes)));
  }

  @POST
  @Path(VerificationConstants.NOTIFY_LEARNING_FAILURE)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @Produces({"application/json", "application/v1+json"})
  public RestResponse<Boolean> notifyFailure(@HeaderParam("Accept") String acceptHeaders,
      @QueryParam("taskId") String taskId, @Valid LearningEngineError learningEngineError) {
    return new RestResponse<>(learningEngineService.notifyFailure(taskId, learningEngineError));
  }

  /**
   * API to fetch next available task.
   * @param acceptHeaders
   * @return
   */
  @GET
  @Path("/get-next-task")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @Produces({"application/json", "application/v1+json"})
  public RestResponse<LearningEngineAnalysisTask> getNextTask(@HeaderParam("Accept") String acceptHeaders) {
    return new RestResponse<>(learningEngineService.getNextLearningEngineAnalysisTask(parseApisVersion(acceptHeaders)));
  }
}
