package software.wings.resources;

import static software.wings.utils.Misc.parseApisVersion;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.intfc.LearningEngineService;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 09/05/17.
 */
@Api(LearningEngineService.RESOURCE_URL)
@Path("/" + LearningEngineService.RESOURCE_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class LearningEngineResource {
  @Inject private LearningEngineService learningEngineService;

  @GET
  @Path("/get-next-task")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @Produces({"application/json", "application/v1+json"})
  public RestResponse<LearningEngineAnalysisTask> getNextTask(@HeaderParam("Accept") String acceptHeaders) {
    return new RestResponse<>(learningEngineService.getNextLearningEngineAnalysisTask(parseApisVersion(acceptHeaders)));
  }

  @GET
  @Path("/get-next-exp-task")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @Produces({"application/json", "application/v1+json"})
  public RestResponse<LearningEngineExperimentalAnalysisTask> getNextExperimentalTask(
      @QueryParam("experimentName") String experimentName, @HeaderParam("Accept") String acceptHeaders) {
    return new RestResponse<>(learningEngineService.getNextLearningEngineExperimentalAnalysisTask(
        experimentName, parseApisVersion(acceptHeaders)));
  }
}
