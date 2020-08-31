package io.harness.cvng.core.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.HOST_RECORD_RESOURCE_PATH;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.HostRecordDTO;
import io.harness.cvng.core.services.api.HostRecordService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.swagger.annotations.Api;
import retrofit2.http.Body;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(HOST_RECORD_RESOURCE_PATH)
@Path(HOST_RECORD_RESOURCE_PATH)
@Produces("application/json")
@ExposeInternalException
public class HostRecordResource {
  @Inject private HostRecordService hostRecordService;

  @POST
  @Timed
  @ExceptionMetered
  @DelegateAuth
  public RestResponse<Void> saveHostRecords(
      @QueryParam("accountId") @NotNull String accountId, @NotNull @Valid @Body List<HostRecordDTO> hostRecordDTOs) {
    hostRecordService.save(hostRecordDTOs);
    return new RestResponse<>(null);
  }
}
