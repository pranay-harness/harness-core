package io.harness.cvng.analysis.resource;

import static io.harness.cvng.CVConstants.LEARNING_RESOURCE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.services.api.LearningEngineAnalysisService;
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
public class LearningEngineResource {
  @Inject LearningEngineAnalysisService learningEngineAnalysisService;

  @GET
  @Path("next-cv-task")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<LearningEngineTask> getNextTask(@QueryParam("taskTypes") List<LearningEngineTaskType> taskTypes) {
    if (taskTypes == null) {
      return new RestResponse<>(learningEngineAnalysisService.getNextAnalysisTask());
    }
    return new RestResponse<>(learningEngineAnalysisService.getNextAnalysisTask(taskTypes));
  }
}
