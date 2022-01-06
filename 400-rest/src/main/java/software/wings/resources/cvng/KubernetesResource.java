/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.cvng;

import static io.harness.cvng.core.services.CVNextGenConstants.KUBERNETES_RESOURCE;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.perpetualtask.CVDataCollectionTaskService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.kubernetes.client.openapi.ApiException;
import io.swagger.annotations.Api;
import java.util.List;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import retrofit2.http.Body;

@Api(KUBERNETES_RESOURCE)
@Path(KUBERNETES_RESOURCE)
@Produces("application/json")
@Slf4j
@LearningEngineAuth
@ExposeInternalException(withStackTrace = true)
public class KubernetesResource {
  @Inject private CVDataCollectionTaskService dataCollectionTaskService;

  @POST
  @Path("namespaces")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getNamespaces(@QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("filter") String filter, @Body DataCollectionConnectorBundle bundle) throws ApiException {
    return new RestResponse<>(
        dataCollectionTaskService.getNamespaces(accountId, orgIdentifier, projectIdentifier, filter, bundle));
  }

  @POST
  @Path("workloads")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getWorkloads(@QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("namespace") String namespace, @QueryParam("filter") String filter,
      @Body DataCollectionConnectorBundle bundle) throws ApiException {
    return new RestResponse<>(
        dataCollectionTaskService.getWorkloads(accountId, orgIdentifier, projectIdentifier, namespace, filter, bundle));
  }

  @POST
  @Path("events")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> checkCapabilityToGetEvents(@QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @Body DataCollectionConnectorBundle bundle) throws ApiException {
    return new RestResponse<>(
        dataCollectionTaskService.checkCapabilityToGetEvents(accountId, orgIdentifier, projectIdentifier, bundle));
  }
}
