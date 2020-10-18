package io.harness.ng.core.entitysetupusage.resource;

import com.google.inject.Inject;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/entitySetupUsage")
@Path("entitySetupUsage")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class EntitySetupUsageResource {
  private static final String IS_REFERRED_ENTITY = "IsReferredEntity";
  EntitySetupUsageService entitySetupUsageService;

  @GET
  @ApiOperation(value = "Get Entities referring this resouce", nickname = "listReferredByEntities")
  public ResponseDTO<Page<EntitySetupUsageDTO>> list(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String referredEntityIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    return ResponseDTO.newResponse(entitySetupUsageService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier, searchTerm));
  }

  @GET
  @Path("/isEntityReferenced")
  @ApiOperation(value = "Returns true if the entity is referenced by other resource", nickname = "isEntityReferenced")
  public ResponseDTO<Boolean> isEntityReferenced(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier) {
    return ResponseDTO.newResponse(
        entitySetupUsageService.isEntityReferenced(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }

  @POST
  @ApiOperation(value = "Saves the entity reference", nickname = "postEntitySetupUsage")
  public ResponseDTO<EntitySetupUsageDTO> save(EntitySetupUsageDTO entitySetupUsageDTO) {
    // todo @Deepak : The api should not be avaialble to other users like UI/clients, but should be available to other
    // microservices
    return ResponseDTO.newResponse(entitySetupUsageService.save(entitySetupUsageDTO));
  }

  @DELETE
  @ApiOperation(value = "Deletes the entity reference", nickname = "deleteEntitySetupUsage")
  public ResponseDTO<Boolean> delete(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @QueryParam(IS_REFERRED_ENTITY) Boolean isReferredEntity) {
    return ResponseDTO.newResponse(entitySetupUsageService.delete(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, isReferredEntity));
  }
}