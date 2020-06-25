package io.harness.cvng.core.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.DELEGATE_DATA_COLLECTION_TASK;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.app.cvng.api.DataCollectionTaskService;
import io.harness.cvng.core.services.entities.DataCollectionTask;
import io.harness.cvng.core.services.entities.DataCollectionTask.DataCollectionTaskResult;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.swagger.annotations.Api;

import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(DELEGATE_DATA_COLLECTION_TASK)
@Path(DELEGATE_DATA_COLLECTION_TASK)
@Produces("application/json")
public class DataCollectionTaskResource {
  @Inject private DataCollectionTaskService dataCollectionTaskService;

  @GET
  @Path("next-task")
  @Timed
  @ExceptionMetered
  @DelegateAuth
  public RestResponse<Optional<DataCollectionTask>> getNextTask(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("cvConfigId") String cvConfigId) {
    return new RestResponse<>(dataCollectionTaskService.getNextTask(accountId, cvConfigId));
  }

  @POST
  @Path("update-status")
  @Timed
  @ExceptionMetered
  @DelegateAuth
  public RestResponse<Void> updateTaskStatus(
      @QueryParam("accountId") @NotNull String accountId, DataCollectionTaskResult dataCollectionTaskResult) {
    dataCollectionTaskService.updateTaskStatus(dataCollectionTaskResult);
    return new RestResponse<>(null);
  }
}
