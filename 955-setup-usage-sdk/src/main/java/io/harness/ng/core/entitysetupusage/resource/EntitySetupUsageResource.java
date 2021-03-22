package io.harness.ng.core.entitysetupusage.resource;

import static io.harness.NGConstants.REFERRED_BY_ENTITY_FQN;
import static io.harness.NGConstants.REFERRED_BY_ENTITY_TYPE;
import static io.harness.NGConstants.REFERRED_ENTITY_FQN;
import static io.harness.NGConstants.REFERRED_ENTITY_TYPE;
import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.constraints.Max;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;

@Api("/entitySetupUsage")
@Path("entitySetupUsage")
@Produces({"application/json"})
@Consumes({"application/json"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@NextGenManagerAuth
@OwnedBy(DX)
public class EntitySetupUsageResource {
  EntitySetupUsageService entitySetupUsageService;

  @GET
  @ApiOperation(value = "Get Entities referring this resource", nickname = "listReferredByEntities")
  public ResponseDTO<Page<EntitySetupUsageDTO>> list(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") @Max(100) int size,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @QueryParam(REFERRED_ENTITY_TYPE) EntityType entityType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    return ResponseDTO.newResponse(entitySetupUsageService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, identifier, entityType, searchTerm));
  }

  @GET
  @Path("internal")
  @ApiOperation(value = "Get Entities referring this resource", nickname = "listAllEntityUsage", hidden = true)
  public ResponseDTO<Page<EntitySetupUsageDTO>> listAllEntityUsage(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(REFERRED_ENTITY_FQN) String referredEntityFQN,
      @QueryParam(REFERRED_ENTITY_TYPE) EntityType entityType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    return ResponseDTO.newResponse(entitySetupUsageService.listAllEntityUsage(
        page, size, accountIdentifier, referredEntityFQN, entityType, searchTerm));
  }

  @GET
  @Path("internal/listAllReferredUsages")
  @ApiOperation(value = "Get Entities referred by this resource", nickname = "listAllReferredUsages", hidden = true)
  public ResponseDTO<List<EntitySetupUsageDTO>> listAllReferredUsages(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(REFERRED_BY_ENTITY_FQN) String referredByEntityFQN,
      @QueryParam(REFERRED_ENTITY_TYPE) EntityType referredEntityType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    return ResponseDTO.newResponse(entitySetupUsageService.listAllReferredUsages(
        page, size, accountIdentifier, referredByEntityFQN, referredEntityType, searchTerm));
  }

  @GET
  @Path("/internal/isEntityReferenced")
  @ApiOperation(value = "Returns true if the entity is referenced by other resource", nickname = "isEntityReferenced",
      hidden = true)
  public ResponseDTO<Boolean>
  isEntityReferenced(@NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(REFERRED_ENTITY_FQN) String referredEntityFQN,
      @QueryParam(REFERRED_ENTITY_TYPE) EntityType entityType) {
    return ResponseDTO.newResponse(
        entitySetupUsageService.isEntityReferenced(accountIdentifier, referredEntityFQN, entityType));
  }

  // use eveent fmwk
  @POST
  @Path("internal")
  @Deprecated
  @ApiOperation(value = "Saves the entity reference", nickname = "postEntitySetupUsage", hidden = true)
  public ResponseDTO<EntitySetupUsageDTO> save(EntitySetupUsageDTO entitySetupUsageDTO) {
    return ResponseDTO.newResponse(entitySetupUsageService.save(entitySetupUsageDTO));
  }

  // use event fmwk
  @DELETE
  @Path("internal")
  @Deprecated
  @ApiOperation(value = "Deletes the entity reference record", nickname = "deleteEntitySetupUsage", hidden = true)
  public ResponseDTO<Boolean> delete(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(REFERRED_ENTITY_FQN) String referredEntityFQN,
      @QueryParam(REFERRED_ENTITY_TYPE) EntityType referredEntityType,
      @QueryParam(REFERRED_BY_ENTITY_FQN) String referredByEntityFQN,
      @QueryParam(REFERRED_BY_ENTITY_TYPE) EntityType referredByEntityType) {
    return ResponseDTO.newResponse(entitySetupUsageService.delete(
        accountIdentifier, referredEntityFQN, referredEntityType, referredByEntityFQN, referredByEntityType));
  }

  // use event fmwk
  @Deprecated
  @DELETE
  @Path("/internal/deleteAllReferredByRecords")
  @ApiOperation(value = "Deletes the entity reference records for referredByEntity",
      nickname = "deleteAllReferredByEntityRecords", hidden = true)
  public ResponseDTO<Boolean>
  deleteAllReferredByEntityRecords(@NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(REFERRED_BY_ENTITY_FQN) String referredByEntityFQN,
      @QueryParam(REFERRED_BY_ENTITY_TYPE) EntityType referredByEntityType) {
    return ResponseDTO.newResponse(entitySetupUsageService.deleteAllReferredByEntityRecords(
        accountIdentifier, referredByEntityFQN, referredByEntityType));
  }
}
