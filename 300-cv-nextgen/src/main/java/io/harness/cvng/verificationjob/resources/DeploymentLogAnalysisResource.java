package io.harness.cvng.verificationjob.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.ng.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("deployment-log-analysis")
@Path("deployment-log-analysis")
@Produces("application/json")
@ExposeInternalException
public class DeploymentLogAnalysisResource {
  @Inject private DeploymentLogAnalysisService deploymentLogAnalysisService;

  @Produces({"application/json", "application/v1+json"})
  @GET
  @Path("/{verificationJobInstanceId}/clusters")
  @Timed
  @ExceptionMetered
  public RestResponse<List<LogAnalysisClusterChartDTO>> getMetrics(
      @PathParam("verificationJobInstanceId") String verificationJobInstanceId,
      @QueryParam("accountId") String accountId) {
    return new RestResponse(deploymentLogAnalysisService.getLogAnalysisClusters(accountId, verificationJobInstanceId));
  }

  @Path("/{verificationJobInstanceId}")
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<LogAnalysisClusterDTO>> getMetrics(
      @PathParam("verificationJobInstanceId") String verificationJobInstanceId,
      @QueryParam("accountId") String accountId, @QueryParam("label") Integer label,
      @QueryParam("pageNumber") int pageNumber) {
    return new RestResponse(
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, label, pageNumber));
  }
}
