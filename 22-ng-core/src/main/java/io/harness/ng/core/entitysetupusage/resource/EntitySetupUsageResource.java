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
@Produces({"application/json"})
@Consumes({"application/json"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class EntitySetupUsageResource {
  private static final String REFERRED_ENTITY_FQN = "referredEntityFQN";
  private static final String REFERRED_BY_ENTITY_FQN = "referredByEntityFQN";
  private static final String ENTITY_FQN = "entityFQN";
  EntitySetupUsageService entitySetupUsageService;

  @GET
  @ApiOperation(value = "Get Entities referring this resource", nickname = "listReferredByEntities")
  public ResponseDTO<Page<EntitySetupUsageDTO>> list(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    return ResponseDTO.newResponse(entitySetupUsageService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, identifier, searchTerm));
  }

  @GET
  @Path("internal")
  @ApiOperation(value = "Get Entities referring this resource", nickname = "listAllEntityUsage")
  public ResponseDTO<Page<EntitySetupUsageDTO>> listAllEntityUsage(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(REFERRED_ENTITY_FQN) String referredEntityFQN,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    return ResponseDTO.newResponse(
        entitySetupUsageService.listAllEntityUsage(page, size, accountIdentifier, referredEntityFQN, searchTerm));
  }

  @GET
  @Path("/internal/isEntityReferenced")
  @ApiOperation(value = "Returns true if the entity is referenced by other resource", nickname = "isEntityReferenced")
  public ResponseDTO<Boolean> isEntityReferenced(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(REFERRED_ENTITY_FQN) String referredEntityFQN) {
    return ResponseDTO.newResponse(entitySetupUsageService.isEntityReferenced(accountIdentifier, referredEntityFQN));
  }

  @POST
  @Path("internal")
  @ApiOperation(value = "Saves the entity reference", nickname = "postEntitySetupUsage")
  public ResponseDTO<EntitySetupUsageDTO> save(EntitySetupUsageDTO entitySetupUsageDTO) {
    // todo @Deepak : The api should not be avaialble to other users like UI/clients, but should be available to other
    // microservices
    return ResponseDTO.newResponse(entitySetupUsageService.save(entitySetupUsageDTO));
  }

  @DELETE
  @Path("internal")
  @ApiOperation(value = "Deletes the entity reference record", nickname = "deleteEntitySetupUsage")
  public ResponseDTO<Boolean> delete(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(REFERRED_ENTITY_FQN) String referredEntityFQN,
      @QueryParam(REFERRED_BY_ENTITY_FQN) String referredByEntityFQN) {
    return ResponseDTO.newResponse(
        entitySetupUsageService.delete(accountIdentifier, referredEntityFQN, referredByEntityFQN));
  }

  @DELETE
  @Path("/internal/deleteAllReferredByRecords")
  @ApiOperation(value = "Deletes the entity reference records for referredByEntity",
      nickname = "deleteAllReferredByEntityRecords")
  public ResponseDTO<Boolean>
  deleteAllReferredByEntityRecords(@NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(REFERRED_BY_ENTITY_FQN) String referredByEntityFQN) {
    return ResponseDTO.newResponse(
        entitySetupUsageService.deleteAllReferredByEntityRecords(accountIdentifier, referredByEntityFQN));
  }
}
