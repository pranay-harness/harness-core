package io.harness.cvng.core.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.LOG_RECORD_RESOURCE_PATH;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.beans.LogRecordDTO;
import io.harness.cvng.core.services.api.LogRecordService;
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

@Api(LOG_RECORD_RESOURCE_PATH)
@Path(LOG_RECORD_RESOURCE_PATH)
@Produces("application/json")
public class LogRecordResource {
  @Inject private LogRecordService logRecordService;

  @POST
  @Timed
  @ExceptionMetered
  @DelegateAuth
  public RestResponse<Void> saveLogRecords(
      @QueryParam("accountId") @NotNull String accountId, @NotNull @Valid @Body List<LogRecordDTO> logRecords) {
    logRecordService.save(logRecords);
    return new RestResponse<>(null);
  }
}
