package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.entities.CEReportSchedule;
import io.harness.ccm.views.service.CEReportScheduleService;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.eraro.ResponseMessage;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Api("perspectiveReport")
@Path("/perspectiveReport")
@Produces("application/json")
@NextGenManagerAuth
@Slf4j
@Service
@OwnedBy(CE)
public class PerspectiveReportResource {
  private CEReportScheduleService ceReportScheduleService;
  private static final String accountIdPathParam = "{" + NGCommonEntityConstants.ACCOUNT_KEY + "}";

  @Inject
  public PerspectiveReportResource(CEReportScheduleService ceReportScheduleService) {
    this.ceReportScheduleService = ceReportScheduleService;
  }

  @GET
  @Timed
  @Path(accountIdPathParam)
  @ExceptionMetered
  @ApiOperation(value = "Get perspective reports", nickname = "getReportSetting")
  public RestResponse<List<CEReportSchedule>> getReportSetting(@QueryParam("perspectiveId") String perspectiveId,
      @QueryParam("reportId") String reportId, @PathParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId) {
    if (perspectiveId != null) {
      return new RestResponse<>(ceReportScheduleService.getReportSettingByView(perspectiveId, accountId));
    } else if (reportId != null) {
      List<CEReportSchedule> ceList = new ArrayList<>();
      CEReportSchedule rep = ceReportScheduleService.get(reportId, accountId);
      if (rep != null) {
        ceList.add(rep);
      }
      return new RestResponse<>(ceList);
    }
    // INVALID_REQUEST
    RestResponse<List<CEReportSchedule>> rr = new RestResponse<>();
    addResponseMessage(
        rr, ErrorCode.INVALID_REQUEST, Level.ERROR, "ERROR: Invalid request. Either 'viewId' or 'reportId' is needed");
    return rr;
  }

  @DELETE
  @Timed
  @Path(accountIdPathParam)
  @ExceptionMetered
  @ApiOperation(value = "Delete perspective reports", nickname = "deleteReportSetting")
  public RestResponse<String> deleteReportSetting(@QueryParam("reportId") String reportId,
      @QueryParam("perspectiveId") String perspectiveId,
      @PathParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId) {
    if (perspectiveId != null) {
      ceReportScheduleService.deleteAllByView(perspectiveId, accountId);
      return new RestResponse<>("Successfully deleted the record");
    } else if (reportId != null) {
      ceReportScheduleService.delete(reportId, accountId);
      return new RestResponse<>("Successfully deleted the record");
    }
    // INVALID_REQUEST
    RestResponse<String> rr = new RestResponse<>();
    addResponseMessage(
        rr, ErrorCode.INVALID_REQUEST, Level.ERROR, "ERROR: Invalid request. Either 'viewId' or 'reportId' is needed");
    return rr;
  }

  @POST
  @Path(accountIdPathParam)
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Create perspective reports", nickname = "createReportSetting")
  public RestResponse<List<CEReportSchedule>> createReportSetting(
      @PathParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId, @Valid @RequestBody CEReportSchedule schedule) {
    List<CEReportSchedule> ceList = new ArrayList<>();
    try {
      CronSequenceGenerator cronSequenceGenerator = new CronSequenceGenerator(schedule.getUserCron());
      ceList.add(ceReportScheduleService.createReportSetting(accountId, schedule));
      return new RestResponse<>(ceList);
    } catch (IllegalArgumentException e) {
      log.error("ERROR", e);
      RestResponse<List<CEReportSchedule>> rr = new RestResponse<>();
      addResponseMessage(
          rr, ErrorCode.INVALID_REQUEST, Level.ERROR, "ERROR: Invalid request. Schedule provided is invalid");
      return rr;
    }
  }

  @PUT
  @Path(accountIdPathParam)
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Update perspective reports", nickname = "updateReportSetting")
  public RestResponse<List<CEReportSchedule>> updateReportSetting(
      @PathParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId, @Valid @RequestBody CEReportSchedule schedule) {
    try {
      CronSequenceGenerator cronSequenceGenerator = new CronSequenceGenerator(schedule.getUserCron());
      return new RestResponse<>(ceReportScheduleService.update(accountId, schedule));
    } catch (IllegalArgumentException e) {
      log.warn(String.valueOf(e));
      RestResponse<List<CEReportSchedule>> rr = new RestResponse<>();
      addResponseMessage(
          rr, ErrorCode.INVALID_REQUEST, Level.ERROR, "ERROR: Invalid request. Schedule provided is invalid");
      return rr;
    }
  }

  private static void addResponseMessage(RestResponse rr, ErrorCode errorCode, Level level, String message) {
    ResponseMessage rm = ResponseMessage.builder().code(errorCode).level(level).message(message).build();

    List<ResponseMessage> responseMessages = rr.getResponseMessages();
    responseMessages.add(rm);
    rr.setResponseMessages(responseMessages);
  }

  private Response prepareResponse(RestResponse restResponse, Response.Status status) {
    return Response.status(status).entity(restResponse).type(MediaType.APPLICATION_JSON).build();
  }
}
