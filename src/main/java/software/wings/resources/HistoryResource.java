package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.History;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.HistoryService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by peeyushaggarwal on 6/20/16.
 */
@Api("history")
@Path("/history")
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class HistoryResource {
  private HistoryService historyService;

  /**
   * Instantiates a new History resource.
   *
   * @param historyService the history service
   */
  @Inject
  public HistoryResource(HistoryService historyService) {
    this.historyService = historyService;
  }

  /**
   * List rest response.
   *
   * @param appId   the app id
   * @param request the request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<History>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<History> request) {
    request.addFilter("appId", appId, EQ);
    return new RestResponse<>(historyService.list(request));
  }

  /**
   * Details rest response.
   *
   * @param appId     the app id
   * @param historyId the history id
   * @return the rest response
   */
  @GET
  @Path("{historyId}")
  public RestResponse<History> get(@QueryParam("appId") String appId, @PathParam("historyId") String historyId) {
    return new RestResponse<>(historyService.get(appId, historyId));
  }
}
