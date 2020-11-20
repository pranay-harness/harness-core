package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.perpetualtask.example.SamplePTaskService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Slf4j
@Api("perpetual-task")
@Path("/perpetual-task")
@Produces(MediaType.APPLICATION_JSON)
public class SamplePTaskResource {
  @Inject private SamplePTaskService samplePTaskService;

  @Inject
  public SamplePTaskResource(SamplePTaskService samplePTaskService) {
    this.samplePTaskService = samplePTaskService;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<String> create(@QueryParam("accountId") String accountId,
      @QueryParam("country") String countryName, @QueryParam("population") int population) {
    return new RestResponse<>(samplePTaskService.create(accountId, countryName, population));
  }

  @PUT
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> update(@QueryParam("accountId") String accountId, @QueryParam("taskId") String taskId,
      @QueryParam("country") String countryName, @QueryParam("population") int population) {
    samplePTaskService.update(accountId, taskId, countryName, population);
    return new RestResponse<>();
  }
}
