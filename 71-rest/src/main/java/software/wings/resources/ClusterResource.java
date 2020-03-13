package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("clusters")
@Path("/clusters")
@Produces("application/json")
public class ClusterResource {
  private ClusterRecordService clusterRecordService;

  @Inject
  public ClusterResource(ClusterRecordService clusterRecordService) {
    this.clusterRecordService = clusterRecordService;
  }

  @GET
  @Path("{id}")
  @Timed
  @ExceptionMetered
  public RestResponse<ClusterRecord> get(@QueryParam("accountId") String accountId, @PathParam("id") String clusterId) {
    return new RestResponse<>(clusterRecordService.get(clusterId));
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<ClusterRecord>> list(@NotEmpty @QueryParam("accountId") String accountId,
      @QueryParam("cloudProviderId") String cloudProviderId, @QueryParam("count") Integer count,
      @QueryParam("startIndex") Integer startIndex) {
    return new RestResponse<>(clusterRecordService.list(accountId, cloudProviderId, false, count, startIndex));
  }
}
