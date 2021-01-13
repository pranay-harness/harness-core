package io.harness.ngtriggers.resource;

import static io.harness.utils.PageUtils.getNGPageResponse;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.NGTriggerDetailsResponseDTO;
import io.harness.ngtriggers.beans.dto.NGTriggerResponseDTO;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityKeys;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.mapper.TriggerFilterHelper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@Api("triggers")
@Path("triggers")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
public class NGTriggerResource {
  private final NGTriggerService ngTriggerService;
  private final NGTriggerElementMapper ngTriggerElementMapper;

  @POST
  @ApiImplicitParams({
    @ApiImplicitParam(dataTypeClass = NGTriggerConfig.class,
        dataType = "io.harness.ngtriggers.beans.config.NGTriggerConfig", paramType = "body")
  })
  @ApiOperation(value = "Create Trigger", nickname = "createTrigger")
  public ResponseDTO<NGTriggerResponseDTO>
  create(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) {
    NGTriggerEntity createdEntity = null;
    try {
      TriggerDetails triggerDetails =
          ngTriggerElementMapper.toTriggerDetails(accountIdentifier, orgIdentifier, projectIdentifier, yaml);
      // ngTriggerService.sanitizeRuntimeInputForTrigger(triggerDetails);
      createdEntity = ngTriggerService.create(triggerDetails.getNgTriggerEntity());
      return ResponseDTO.newResponse(
          createdEntity.getVersion().toString(), ngTriggerElementMapper.toResponseDTO(createdEntity));
    } catch (Exception e) {
      throw new InvalidRequestException("Failed while Saving Trigger: " + e.getMessage());
    }
  }

  @GET
  @Path("/{triggerIdentifier}")
  @ApiOperation(value = "Gets a trigger by identifier", nickname = "getTrigger")
  public ResponseDTO<NGTriggerResponseDTO> get(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam("targetIdentifier") String targetIdentifier,
      @PathParam("triggerIdentifier") String triggerIdentifier) {
    Optional<NGTriggerEntity> ngTriggerEntity = ngTriggerService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, targetIdentifier, triggerIdentifier, false);
    return ResponseDTO.newResponse(ngTriggerEntity.get().getVersion().toString(),
        ngTriggerEntity.map(ngTriggerElementMapper::toResponseDTO).orElse(null));
  }

  @PUT
  @Path("/{triggerIdentifier}")
  @ApiImplicitParams({
    @ApiImplicitParam(dataTypeClass = NGTriggerConfig.class,
        dataType = "io.harness.ngtriggers.beans.config.NGTriggerConfig", paramType = "body")
  })
  @ApiOperation(value = "Update a trigger by identifier", nickname = "updateTrigger")
  public ResponseDTO<NGTriggerResponseDTO>
  update(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam("triggerIdentifier") String triggerIdentifier,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) {
    try {
      TriggerDetails triggerDetails =
          ngTriggerElementMapper.toTriggerDetails(accountIdentifier, orgIdentifier, projectIdentifier, yaml);
      // ngTriggerService.sanitizeRuntimeInputForTrigger(triggerDetails);
      triggerDetails.getNgTriggerEntity().setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);

      NGTriggerEntity updatedEntity = ngTriggerService.update(triggerDetails.getNgTriggerEntity());
      return ResponseDTO.newResponse(
          updatedEntity.getVersion().toString(), ngTriggerElementMapper.toResponseDTO(updatedEntity));
    } catch (Exception e) {
      throw new InvalidRequestException("Failed while updating Trigger: " + e.getMessage());
    }
  }

  @PUT
  @Path("{triggerIdentifier}/status")
  @ApiOperation(value = "Update a trigger's status by identifier", nickname = "updateTriggerStatus")
  public ResponseDTO<Boolean> updateTriggerStatus(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam("targetIdentifier") String targetIdentifier,
      @PathParam("triggerIdentifier") String triggerIdentifier, @NotNull @QueryParam("status") boolean status) {
    Optional<NGTriggerEntity> ngTriggerEntity = ngTriggerService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, targetIdentifier, triggerIdentifier, false);
    return ResponseDTO.newResponse(ngTriggerService.updateTriggerStatus(ngTriggerEntity.get(), status));
  }

  @DELETE
  @Path("{triggerIdentifier}")
  @ApiOperation(value = "Delete a trigger by identifier", nickname = "deleteTrigger")
  public ResponseDTO<Boolean> delete(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam("targetIdentifier") String targetIdentifier,
      @PathParam("triggerIdentifier") String triggerIdentifier) {
    return ResponseDTO.newResponse(ngTriggerService.delete(accountIdentifier, orgIdentifier, projectIdentifier,
        targetIdentifier, triggerIdentifier, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }

  @GET
  @ApiOperation(value = "Gets Triggers list for target", nickname = "getTriggerListForTarget")
  public ResponseDTO<PageResponse<NGTriggerDetailsResponseDTO>> getListForTarget(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam("targetIdentifier") String targetIdentifier, @QueryParam("filter") String filterQuery,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("25") int size,
      @QueryParam("sort") List<String> sort, @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    Criteria criteria = TriggerFilterHelper.createCriteriaForGetList(
        accountIdentifier, orgIdentifier, projectIdentifier, targetIdentifier, null, searchTerm, false);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, NGTriggerEntityKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }

    return ResponseDTO.newResponse(getNGPageResponse(
        ngTriggerService.list(criteria, pageRequest)
            .map(triggerEntity -> ngTriggerElementMapper.toNGTriggerDetailsResponseDTO(triggerEntity, false))));
  }

  @GET
  @Path("{triggerIdentifier}/details")
  @ApiOperation(value = "Gets Triggers list for target", nickname = "getTriggerDetails")
  public ResponseDTO<NGTriggerDetailsResponseDTO> getTriggerDetails(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam("triggerIdentifier") String triggerIdentifier,
      @NotNull @QueryParam("targetIdentifier") String targetIdentifier) {
    Optional<NGTriggerEntity> ngTriggerEntity = ngTriggerService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, targetIdentifier, triggerIdentifier, false);

    if (!ngTriggerEntity.isPresent()) {
      return ResponseDTO.newResponse(null);
    }

    return ResponseDTO.newResponse(ngTriggerEntity.get().getVersion().toString(),
        ngTriggerElementMapper.toNGTriggerDetailsResponseDTO(ngTriggerEntity.get(), true));
  }

  @GET
  @Path("/triggersList")
  @ApiOperation(value = "Gets Triggers list for Repo URL", nickname = "getTriggerListForRepoURL")
  public ResponseDTO<PageResponse<NGTriggerResponseDTO>> getListForRepoURL(
      @NotNull @QueryParam("repoURL") String repoURL, @QueryParam("filter") String filterQuery,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("25") int size,
      @QueryParam("sort") List<String> sort, @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    Criteria criteria =
        TriggerFilterHelper.createCriteriaForWebhookTriggerGetList(null, repoURL, searchTerm, false, false);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, NGTriggerEntityKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }
    Page<NGTriggerResponseDTO> triggers =
        ngTriggerService.list(criteria, pageRequest).map(ngTriggerElementMapper::toResponseDTO);
    return ResponseDTO.newResponse(getNGPageResponse(triggers));
  }
}
