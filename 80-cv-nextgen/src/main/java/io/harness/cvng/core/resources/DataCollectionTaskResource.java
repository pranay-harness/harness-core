package io.harness.cvng.core.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.DELEGATE_DATA_COLLECTION_TASK;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.beans.DataCollectionTaskDTO;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
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
  public RestResponse<Optional<DataCollectionTaskDTO>> getNextTask(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("cvConfigId") String cvConfigId) {
    return new RestResponse<>(dataCollectionTaskService.getNextTaskDTO(accountId, cvConfigId));
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
