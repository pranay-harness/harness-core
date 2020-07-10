package io.harness.cvng.analysis.resource;

import static io.harness.cvng.CVConstants.LEARNING_RESOURCE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.swagger.annotations.Api;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(LEARNING_RESOURCE)
@Path(LEARNING_RESOURCE)
@Produces("application/json")
public class LearningEngineTaskResource {
  @Inject LearningEngineTaskService learningEngineTaskService;

  @GET
  @Path("next-cv-task")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<LearningEngineTask> getNextTask(@QueryParam("taskTypes") List<LearningEngineTaskType> taskTypes) {
    if (taskTypes == null) {
      return new RestResponse<>(learningEngineTaskService.getNextAnalysisTask());
    }
    return new RestResponse<>(learningEngineTaskService.getNextAnalysisTask(taskTypes));
  }

  @GET
  @Path("mark-failure")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Boolean> markFailure(@QueryParam("taskId") String taskId) {
    learningEngineTaskService.markFailure(taskId);
    return new RestResponse<>(true);
  }
}
