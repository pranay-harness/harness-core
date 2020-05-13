package software.wings.resources.ci;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.common.CICommonEndpointConstants;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.service.impl.ci.CIDelegateTaskHelperService;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 *  This is temporary resource class for accepting CI tasks.
 *  Temporarily we are using Learning engine token
 *
 *  We have to remove all query params and pass it as part of class
 *  Use delegate microservice eventually instead of this class
 */

@Path("/ci")
@Produces("application/json")
public class CIServiceHelperResource {
  @Inject private CIDelegateTaskHelperService ciDelegateTaskHelperService;

  @POST
  @Path(CICommonEndpointConstants.CI_SETUP_ENDPOINT)
  @Timed
  @LearningEngineAuth
  public RestResponse<K8sTaskExecutionResponse> setBuildEnv(@QueryParam("k8ConnectorName") String k8ConnectorName,
      @QueryParam("gitConnectorName") String gitConnectorName, CIK8PodParams<CIK8ContainerParams> podParams) {
    return new RestResponse<K8sTaskExecutionResponse>(
        ciDelegateTaskHelperService.setBuildEnv(k8ConnectorName, gitConnectorName, podParams));
  }

  @POST
  @Path(CICommonEndpointConstants.CI_COMMAND_EXECUTION_ENDPOINT)
  @Timed
  @LearningEngineAuth
  public RestResponse<K8sTaskExecutionResponse> executeBuildCommand(
      @QueryParam("k8ConnectorName") String k8ConnectorName, K8ExecCommandParams params) {
    return new RestResponse<K8sTaskExecutionResponse>(
        ciDelegateTaskHelperService.executeBuildCommand(k8ConnectorName, params));
  }

  @DELETE
  @Path(CICommonEndpointConstants.CI_CLEANUP_ENDPOINT)
  @Timed
  @LearningEngineAuth
  public RestResponse<K8sTaskExecutionResponse> cleanup(@QueryParam("k8ConnectorName") String k8ConnectorName) {
    return new RestResponse<K8sTaskExecutionResponse>(ciDelegateTaskHelperService.cleanupEnv(k8ConnectorName));
  }
}
