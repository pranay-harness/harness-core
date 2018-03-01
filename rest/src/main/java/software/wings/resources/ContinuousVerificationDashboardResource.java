package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.github.reinert.jjschema.SchemaIgnore;
import io.swagger.annotations.Api;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationService;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("cvdash")
@Path("/cvdash")
@Produces("application/json")
@AuthRule(PermissionAttribute.ResourceType.SERVICE)
public class ContinuousVerificationDashboardResource {
  @Transient @Inject @SchemaIgnore protected ContinuousVerificationService continuousVerificationService;

  @GET
  @Path("/get-records")
  @Timed
  @ExceptionMetered
  public RestResponse<
      Map<Long, TreeMap<String, Map<String, Map<String, Map<String, List<ContinuousVerificationExecutionMetaData>>>>>>>
  getCVExecutionRecords(@QueryParam("accountId") String accountId, @QueryParam("beginEpochTs") long beginEpochTs,
      @QueryParam("endEpochTs") long endEpochTs) throws ParseException {
    return new RestResponse<>(
        continuousVerificationService.getCVExecutionMetaData(accountId, beginEpochTs, endEpochTs));
  }
}
