package io.harness.pms.downloadlogs.resources;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.*;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.downloadlogs.beans.entity.DownloadLogsEntity;
import io.harness.pms.downloadlogs.beans.resource.DownloadLogsRequestBody;
import io.harness.pms.downloadlogs.beans.resource.DownloadLogsResponseDTO;
import io.harness.pms.downloadlogs.mappers.DownloadLogsMapper;
import io.harness.pms.downloadlogs.service.DownloadLogsService;
import io.harness.pms.rbac.PipelineRbacPermissions;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.time.OffsetDateTime;
import java.util.Date;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Api("/downloadLogs")
@Path("/downloadLogs")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@PipelineServiceAuth
@Slf4j
public class DownloadLogsResource {
  private final DownloadLogsService downloadLogsService;
  private final AccessControlClient accessControlClient;

  @PUT
  @ApiOperation(value = "Generate log download link", nickname = "GenerateLink")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<DownloadLogsResponseDTO> generateLink(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineId,
      @BeanParam DownloadLogsRequestBody downloadLogsRequestBody) {
    // TODO: Verify logKey

    downloadLogsRequestBody.setCreatedAt(Date.from(OffsetDateTime.now().toInstant()));

    DownloadLogsEntity entity = DownloadLogsMapper.toDownloadLogsEntity(downloadLogsRequestBody.getLogKey(),
        downloadLogsRequestBody.getCreatedAt(), downloadLogsRequestBody.getTimeToLive(), accountId, orgId, projectId,
        pipelineId);
    DownloadLogsEntity createdEntity = downloadLogsService.create(entity);

    String downloadId = createdEntity.getUuid();
    // TODO: Call function to generate Download link
    String downloadLink = "myDummyDownloadLink" + downloadId;

    return ResponseDTO.newResponse(createdEntity.getVersion().toString(),
        DownloadLogsMapper.toDownloadLogsResponseDTO(downloadLink, createdEntity.getValidUntil()));
  }

  @GET
  @Path("/{downloadId}")
  @ApiOperation(value = "Download log zip file", nickname = "DownloadLink")
  public void returnLink(@PathParam("downloadId") String downloadId) {
    DownloadLogsEntity downloadLogsEntity = downloadLogsService.getByDownloadId(downloadId);

    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(downloadLogsEntity.getAccountId(), downloadLogsEntity.getOrgIdentifier(),
            downloadLogsEntity.getProjectIdentifier()),
        Resource.of("PIPELINE", downloadLogsEntity.getPipelineIdentifier()), PipelineRbacPermissions.PIPELINE_VIEW);
    // TODO: Verify logKey

    // TODO: Functionality for GET call
    /*
    String version = "0";
    if (downloadLogsEntity.isPresent()) {
      version = downloadLogsEntity.get().getVersion().toString();
    }
    return ResponseDTO.newResponse(
        version, downloadLogsEntity.map(DownloadLogsMapper::toDownloadLogsResponseDTO).orElse(null));
    */
  }
}
