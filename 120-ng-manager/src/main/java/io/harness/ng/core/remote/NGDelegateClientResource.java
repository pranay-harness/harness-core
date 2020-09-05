package io.harness.ng.core.remote;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.ManagerDelegateServiceDriver;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.ng.core.perpetualtask.sample.SampleRemotePTaskManager;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/delegate-tasks")
@Api("/delegate-tasks")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
@Slf4j
public class NGDelegateClientResource {
  private static final String HTTP_URL_200 = "http://httpstat.us/200";
  private final ManagerDelegateServiceDriver managerDelegateServiceDriver;
  private final SampleRemotePTaskManager sampleRemotePTaskManager;

  @GET
  @ApiOperation(value = "Create a delegate tasks", nickname = "postDelegate")
  public DelegateResponseData create(@QueryParam("accountId") @NotBlank String accountId) {
    Map<String, String> setupAbstractions = ImmutableMap.of("accountId", accountId);
    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType("HTTP")
            .timeout(TimeUnit.MINUTES.toMillis(1))
            .parameters(new Object[] {HttpTaskParameters.builder().method("GET").url(HTTP_URL_200).build()})
            .build();
    return this.managerDelegateServiceDriver.sendTask(accountId, setupAbstractions, taskData);
  }

  @POST
  @Path("/perpetual-tasks/sample")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Create a sample perpetual task", nickname = "createSamplePTask")
  public RestResponse<String> createSamplePTask(@QueryParam("accountId") String accountId,
      @QueryParam("country") String countryName, @QueryParam("population") int population) {
    return new RestResponse<>(sampleRemotePTaskManager.create(accountId, countryName, population));
  }

  @DELETE
  @Path("/perpetual-tasks/sample/{taskId}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "delete sample perpetual task", nickname = "deleteSamplePTask")
  public RestResponse<Boolean> deleteSamplePTask(
      @QueryParam("accountId") String accountId, @PathParam("taskId") String taskId) {
    return new RestResponse<>(sampleRemotePTaskManager.delete(accountId, taskId));
  }

  @PUT
  @Path("/perpetual-tasks/sample/{taskId}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "reset sample perpetual task", nickname = "resetSamplePTask")
  public RestResponse<Boolean> updateSamplePTask(@QueryParam("accountId") String accountId,
      @PathParam("taskId") String taskId, @QueryParam("country") String countryName,
      @QueryParam("population") int population) {
    return new RestResponse<>(sampleRemotePTaskManager.update(accountId, taskId, countryName, population));
  }
}
