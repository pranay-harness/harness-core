package io.harness.cvng.verificationjob.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterType;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.ng.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@NextGenManagerAuth
public class DeploymentLogAnalysisResource {
  @Inject private DeploymentLogAnalysisService deploymentLogAnalysisService;
  private int DEFAULT_PAGE_SIZE = 10;

  @Produces({"application/json", "application/v1+json"})
  @GET
  @Path("/{verificationJobInstanceId}/clusters")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get logs for given verificationJob", nickname = "getLogAnalysisClusters")
  public RestResponse<List<LogAnalysisClusterChartDTO>> getLogAnalysisClusters(
      @PathParam("verificationJobInstanceId") String verificationJobInstanceId,
      @QueryParam("accountId") String accountId, @QueryParam("hostName") String hostName) {
    return new RestResponse(
        deploymentLogAnalysisService.getLogAnalysisClusters(accountId, verificationJobInstanceId, hostName));
  }

  @Path("/{verificationJobInstanceId}")
  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get logs for given verificationJob", nickname = "getLogAnalysisResult")
  public RestResponse<PageResponse<LogAnalysisClusterDTO>> getLogAnalysisResult(
      @PathParam("verificationJobInstanceId") String verificationJobInstanceId,
      @QueryParam("accountId") String accountId, @QueryParam("label") Integer label,
      @QueryParam("pageNumber") int pageNumber, @QueryParam("hostName") String hostName,
      @QueryParam("clusterType") ClusterType clusterType) {
    return new RestResponse(deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, label, pageNumber, DEFAULT_PAGE_SIZE, hostName, clusterType));
  }
}
