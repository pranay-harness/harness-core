package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.CloudWatchService;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 12/15/16.
 */
@Api("cloudwatch")
@Path("/cloudwatch")
@Produces("application/json")
@Timed
@ExceptionMetered
@PublicApi
public class CloudWatchResource {
  @Inject private CloudWatchService cloudWatchService;

  @GET
  @Path("/namespaces")
  public RestResponse<List<String>> listNamespace(@QueryParam("settingId") String setttingId) {
    return new RestResponse<>(cloudWatchService.listNamespaces(setttingId));
  }

  @GET
  @Path("/namespaces/{namespace}/metrics")
  public RestResponse<List<String>> listMetrics(
      @QueryParam("settingId") String setttingId, @PathParam("namespace") String namespace) {
    return new RestResponse<>(cloudWatchService.listMetrics(setttingId, namespace));
  }

  @GET
  @Path("/namespace/{namespace}/metrics/{metric}/dimensions")
  public RestResponse<List<String>> listNamespace(@QueryParam("settingId") String setttingId,
      @PathParam("namespace") String namespace, @PathParam("metric") String metric) {
    return new RestResponse<>(cloudWatchService.listDimensions(setttingId, namespace, metric));
  }
}
