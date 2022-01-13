/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notification.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;
import static io.harness.notification.remote.mappers.TemplateMapper.toDTO;

import io.harness.Team;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.notification.entities.NotificationTemplate;
import io.harness.notification.remote.dto.TemplateDTO;
import io.harness.notification.service.api.NotificationTemplateService;
import io.harness.stream.BoundedInputStream;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import org.glassfish.jersey.media.multipart.FormDataParam;

@OwnedBy(PL)
@Api("templates")
@Path("templates")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class NotificationTemplateResource {
  private final NotificationTemplateService templateService;
  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;

  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @POST
  @ApiOperation(value = "Create a template", nickname = "postTemplate")
  public ResponseDTO<TemplateDTO> createTemplate(@FormDataParam("file") InputStream inputStream,
      @FormDataParam("team") Team team, @FormDataParam("identifier") String identifier,
      @FormDataParam("harnessManaged") Boolean harnessManaged) {
    NotificationTemplate template =
        templateService.create(identifier, team, new BoundedInputStream(inputStream, MAX_FILE_SIZE), harnessManaged);
    return ResponseDTO.newResponse(toDTO(template).orElse(null));
  }

  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @PUT
  @Path("/insertOrUpdate")
  @ApiOperation(value = "Update a template if exists else create", nickname = "insertOrUpdateTemplate")
  public ResponseDTO<TemplateDTO> insertOrUpdateTemplate(@FormDataParam("file") InputStream inputStream,
      @QueryParam("team") Team team, @QueryParam("identifier") String identifier,
      @QueryParam("harnessManaged") Boolean harnessManaged) {
    Optional<NotificationTemplate> optionalTemplate = templateService.getByIdentifierAndTeam(identifier, team);
    NotificationTemplate template;
    BoundedInputStream boundedInputStream = new BoundedInputStream(inputStream, MAX_FILE_SIZE);
    if (optionalTemplate.isPresent()) {
      template = templateService.update(identifier, team, boundedInputStream, harnessManaged).get();
    } else {
      template = templateService.create(identifier, team, boundedInputStream, harnessManaged);
    }
    return ResponseDTO.newResponse(toDTO(template).orElse(null));
  }

  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @PUT
  @Path("/{identifier}")
  @ApiOperation(value = "Update a template", nickname = "putTemplate")
  public ResponseDTO<TemplateDTO> updateTemplate(@FormDataParam("file") InputStream inputStream,
      @QueryParam("team") @NotNull Team team, @PathParam("identifier") String templateIdentifier,
      @PathParam("harnessManaged") Boolean harnessManaged) {
    Optional<NotificationTemplate> templateOptional = templateService.update(
        templateIdentifier, team, new BoundedInputStream(inputStream, MAX_FILE_SIZE), harnessManaged);
    if (templateOptional.isPresent()) {
      return ResponseDTO.newResponse(toDTO(templateOptional.get()).orElse(null));
    } else {
      throw new InvalidRequestException("No such template found." + templateIdentifier, USER);
    }
  }

  @DELETE
  @Path("/{identifier}")
  @ApiOperation(value = "Delete a template", nickname = "deleteTemplate")
  public ResponseDTO<Boolean> deleteTemplate(
      @PathParam("identifier") String templateIdentifier, @QueryParam("team") @NotNull Team team) {
    return ResponseDTO.newResponse(templateService.delete(templateIdentifier, team));
  }

  @GET
  @ApiOperation(value = "Get templates", nickname = "getTemplates")
  public ResponseDTO<List<TemplateDTO>> getTemplates(@QueryParam("team") @NotNull Team team) {
    return ResponseDTO.newResponse(
        templateService.list(team).stream().map(x -> toDTO(x).orElse(null)).collect(Collectors.toList()));
  }

  @GET
  @Path("/{identifier}")
  @ApiOperation(value = "Get template by identifier", nickname = "getTemplate")
  public ResponseDTO<TemplateDTO> getTemplate(
      @PathParam("identifier") String identifier, @QueryParam("team") Team team) {
    return ResponseDTO.newResponse(
        toDTO(templateService.getByIdentifierAndTeam(identifier, team).orElse(null)).orElse(null));
  }
}
