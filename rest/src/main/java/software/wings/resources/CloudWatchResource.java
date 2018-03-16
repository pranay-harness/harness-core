package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.CloudWatchService;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 12/15/16.
 */
@Api("cloudwatch")
@Path("/cloudwatch")
@ExceptionMetered
@Scope(ResourceType.APPLICATION)
public class CloudWatchResource {
  @Inject private CloudWatchService cloudWatchService;

  @GET
  @Path("/namespaces")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listNamespace(
      @QueryParam("settingId") String setttingId, @QueryParam("region") String region) {
    return new RestResponse<>(cloudWatchService.listNamespaces(setttingId, region));
  }

  @GET
  @Path("/namespaces/{namespace}/metrics")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listMetrics(@QueryParam("settingId") String setttingId,
      @QueryParam("region") String region, @PathParam("namespace") String namespace) {
    return new RestResponse<>(cloudWatchService.listMetrics(setttingId, region, namespace));
  }

  @GET
  @Path("/namespace/{namespace}/metrics/{metric}/dimensions")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listNamespace(@QueryParam("settingId") String setttingId,
      @QueryParam("region") String region, @PathParam("namespace") String namespace,
      @PathParam("metric") String metric) {
    return new RestResponse<>(cloudWatchService.listDimensions(setttingId, region, namespace, metric));
  }
}
