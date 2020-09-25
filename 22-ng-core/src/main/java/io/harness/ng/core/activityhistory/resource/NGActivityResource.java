package io.harness.ng.core.activityhistory.resource;

import static io.harness.NGConstants.ACCOUNT_KEY;
import static io.harness.NGConstants.IDENTIFIER_KEY;
import static io.harness.NGConstants.ORG_KEY;
import static io.harness.NGConstants.PAGE_KEY;
import static io.harness.NGConstants.PROJECT_KEY;
import static io.harness.NGConstants.SIZE_KEY;

import com.google.inject.Inject;

import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import io.harness.ng.core.dto.ResponseDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/activityHistory")
@Path("activityHistory")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class NGActivityResource {
  NGActivityService activityHistoryService;

  @GET
  @ApiOperation(value = "Get Activities where this resource was used", nickname = "listEntityUsageActivity")
  public ResponseDTO<Page<NGActivityDTO>> list(@QueryParam(PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(SIZE_KEY) @DefaultValue("100") int size, @NotEmpty @QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(ORG_KEY) String orgIdentifier, @QueryParam(PROJECT_KEY) String projectIdentifier,
      @QueryParam(IDENTIFIER_KEY) String referredEntityIdentifier) {
    return ResponseDTO.newResponse(activityHistoryService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier));
  }

  @POST
  @ApiOperation(value = "Saves the activity", nickname = "postActivity")
  public ResponseDTO<NGActivityDTO> save(NGActivityDTO activityHistory) {
    return ResponseDTO.newResponse(activityHistoryService.save(activityHistory));
  }
}
