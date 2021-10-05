package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.businessMapping.entities.BusinessMapping;
import io.harness.ccm.businessMapping.service.intf.BusinessMappingService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Service;

@Api("business-mapping")
@Path("/business-mapping")
@Produces("application/json")
@NextGenManagerAuth
@Slf4j
@Service
@OwnedBy(CE)
public class BusinessMappingResource {
  @Inject BusinessMappingService businessMappingService;

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Create Business Mapping", nickname = "createBusinessMapping")
  public RestResponse<Boolean> save(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId, BusinessMapping businessMapping) {
    return new RestResponse<>(businessMappingService.save(businessMapping));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get List Of Business Mappings", nickname = "getBusinessMappingList")
  public RestResponse<List<BusinessMapping>> list(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId) {
    return new RestResponse<>(businessMappingService.list(accountId));
  }

  @GET
  @Path("{id}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get Business Mapping", nickname = "getBusinessMapping")
  public RestResponse<BusinessMapping> get(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId, @PathParam("id") String businessMappingId) {
    return new RestResponse<>(businessMappingService.get(businessMappingId, accountId));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Update Business Mapping", nickname = "updateBusinessMapping")
  public RestResponse<String> update(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId, BusinessMapping businessMapping) {
    businessMappingService.update(businessMapping);
    return new RestResponse<>("Successfully updated the Business Mapping");
  }

  @DELETE
  @Path("{id}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Delete Business Mapping", nickname = "deleteBusinessMapping")
  public RestResponse<String> delete(@NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @PathParam("id") String businessMappingId) {
    businessMappingService.delete(businessMappingId, accountId);
    return new RestResponse<>("Successfully deleted the Business Mapping");
  }
}
