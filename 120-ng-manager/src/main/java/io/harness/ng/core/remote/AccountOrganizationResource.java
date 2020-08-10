package io.harness.ng.core.remote;

import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;

import io.harness.beans.NGPageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.services.OrganizationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("account-organizations")
@Path("organizations")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class AccountOrganizationResource {
  private final OrganizationService organizationService;

  @GET
  @ApiOperation(value = "Searches Organizations", nickname = "searchOrganizations")
  public ResponseDTO<NGPageResponse<OrganizationDTO>> search(@QueryParam("accountIdentifier") String accountIdentifier,
      @QueryParam("search") String search, @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("100") int size, @QueryParam("sort") List<String> sort) {
    Criteria criteria = Criteria.where(OrganizationKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(OrganizationKeys.deleted)
                            .ne(Boolean.TRUE);
    if (isNotBlank(search)) {
      criteria.orOperator(Criteria.where(OrganizationKeys.name).regex(search, "i"),
          Criteria.where(OrganizationKeys.tags).regex(search, "i"));
    }
    Page<OrganizationDTO> organizations =
        organizationService.list(criteria, getPageRequest(page, size, sort)).map(OrganizationMapper::writeDto);
    return ResponseDTO.newResponse(getNGPageResponse(organizations));
  }
}
