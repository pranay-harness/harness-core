package io.harness.cvng.core.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.CVNG_LOG_RESOURCE_PATH;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.cvnglog.CVNGLogDTO;
import io.harness.cvng.core.services.api.CVNGLogService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api(CVNG_LOG_RESOURCE_PATH)
@Path(CVNG_LOG_RESOURCE_PATH)
@Produces("application/json")
@ExposeInternalException
public class CVNGLogResource {
  @Inject private CVNGLogService cvngLogService;

  @POST
  @Timed
  @ExceptionMetered
  @DelegateAuth
  @ApiOperation(value = "saves cvng log data", nickname = "saveCVNGLogRecords")
  public RestResponse<Void> saveCVNGLogRecords(
      @QueryParam("accountId") @NotNull String accountId, @NotNull @Valid @Body List<CVNGLogDTO> cvngLogDTORecords) {
    cvngLogService.save(cvngLogDTORecords);
    return new RestResponse<>(null);
  }
}
