
package software.wings.resources.commandlibrary;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rest.RestResponse.Builder.aRestResponse;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.commandlibrary.api.dto.CommandDTO.CommandDTOBuilder;
import io.harness.commandlibrary.api.dto.CommandStoreDTO;
import io.harness.data.structure.EmptyPredicate;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.mongodb.morphia.query.Sort;
import software.wings.api.commandlibrary.EnrichedCommandVersionDTO;
import software.wings.api.commandlibrary.EnrichedCommandVersionDTO.EnrichedCommandVersionDTOBuilder;
import software.wings.beans.commandlibrary.CommandEntity;
import software.wings.beans.commandlibrary.CommandEntity.CommandEntityKeys;
import software.wings.beans.commandlibrary.CommandVersionEntity;
import software.wings.beans.commandlibrary.CommandVersionEntity.CommandVersionsKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.annotations.AuthRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
// command library service apis, will move to another module
@Api("command-stores")
@Path("/command-stores")
@Produces("application/json")
public class CommandStoreResource {
  public static final String HARNESS = "harness";

  @Inject WingsPersistence wingsPersistence;

  @GET
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<List<CommandStoreDTO>> getCommandStores(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(Collections.singletonList(
        CommandStoreDTO.builder().id(HARNESS).description("Harness Command Library").name("Harness Inc").build()));
  }

  @GET
  @Path("{commandStoreId}/commands/categories")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<List<String>> getCommandCategories(
      @QueryParam("accountId") String accountId, @PathParam("commandStoreId") String commandStoreId) {
    return new RestResponse<>(Arrays.asList("Kubernetes", "Azure", "GCP"));
  }

  @GET
  @Path("{commandStoreId}/commands")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<PageResponse<CommandDTO>> listCommands(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreId") String commandStoreId, @BeanParam PageRequest<CommandEntity> pageRequest,
      @QueryParam("cl_implementation_version") Integer clImplementationVersion,
      @QueryParam("category") String category) {
    return aRestResponse().withResource(listCommandsForStore(commandStoreId, pageRequest, category)).build();
  }

  @GET
  @Path("{commandStoreId}/commands/{commandId}")
  @Timed
  @ExceptionMetered
  public RestResponse<CommandDTO> getCommandDetails(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreId") String commandStoreId, @PathParam("commandId") String commandId) {
    return aRestResponse().withResource(getCommandDetails(commandStoreId, commandId)).build();
  }

  @POST
  @Path("{commandStoreId}/commands")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<CommandEntity> saveCommand(
      @QueryParam("accountId") String accountId, CommandEntity commandEntity) {
    final String commandId = wingsPersistence.save(commandEntity);
    return aRestResponse().withResource(wingsPersistence.get(CommandEntity.class, commandId)).build();
  }

  @GET
  @Path("{commandStoreId}/commands/{commandId}/versions/{version}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<EnrichedCommandVersionDTO> getVersionDetails(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreId") String commandStoreId, @PathParam("commandId") String commandId,
      @PathParam("version") String version) {
    final CommandVersionEntity commandVersionEntity = getCommandVersionEntity(commandStoreId, commandId, version);

    return aRestResponse()
        .withResource(populateCommandVersionDTO(EnrichedCommandVersionDTO.builder(), commandVersionEntity).build())
        .build();
  }

  @POST
  @Path("{commandStoreId}/commands/{commandId}/versions")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<CommandVersionEntity> saveCommandVersion(
      @QueryParam("accountId") String accountId, CommandVersionEntity commandVersionEntity) {
    final String commandVersionId = wingsPersistence.save(commandVersionEntity);
    return aRestResponse().withResource(wingsPersistence.get(CommandVersionEntity.class, commandVersionId)).build();
  }

  private PageResponse<CommandDTO> listCommandsForStore(
      String commandStoreId, PageRequest<CommandEntity> pageRequest, String category) {
    pageRequest.addFilter(SearchFilter.builder()
                              .fieldName(CommandEntityKeys.commandStoreId)
                              .op(EQ)
                              .fieldValues(new String[] {commandStoreId})
                              .build());
    if (EmptyPredicate.isNotEmpty(category)) {
      pageRequest.addFilter(SearchFilter.builder()
                                .fieldName(CommandEntityKeys.category)
                                .op(EQ)
                                .fieldValues(new String[] {category})
                                .build());
    }
    final PageResponse<CommandEntity> commandEntitiesPR = listCommandEntity(pageRequest);
    return convert(commandEntitiesPR);
  }

  private PageResponse<CommandDTO> convert(PageResponse<CommandEntity> commandEntitiesPR) {
    return aPageResponse()
        .withTotal(commandEntitiesPR.getTotal())
        .withOffset(commandEntitiesPR.getOffset())
        .withLimit(commandEntitiesPR.getLimit())
        .withResponse(emptyIfNull(commandEntitiesPR.getResponse())
                          .stream()
                          .map(commandEntity
                              -> populateCommandDTO(CommandDTO.builder(), commandEntity,
                                  getCommandVersionEntity(commandEntity.getCommandStoreId(), commandEntity.getUuid(),
                                      commandEntity.getLatestVersion()),
                                  null)
                                     .build())
                          .collect(toList()))
        .build();
  }

  private CommandDTO getCommandDetails(String commandStoreId, String commandId) {
    final CommandEntity commandEntity = getCommandEntity(commandStoreId, commandId);
    if (commandEntity != null) {
      final CommandVersionEntity commandVersionEntity =
          getCommandVersionEntity(commandStoreId, commandId, commandEntity.getLatestVersion());
      final List<CommandVersionEntity> allVersionEntityList =
          getAllVersionEntitiesForCommand(commandStoreId, commandId);
      return populateCommandDTO(CommandDTO.builder(), commandEntity, commandVersionEntity, allVersionEntityList)
          .build();
    }
    return null;
  }

  private List<CommandVersionEntity> getAllVersionEntitiesForCommand(String commandStoreId, String commandId) {
    return emptyIfNull(wingsPersistence.createQuery(CommandVersionEntity.class)
                           .filter(CommandVersionsKeys.commandStoreId, commandStoreId)
                           .filter(CommandVersionsKeys.commandId, commandId)
                           .order(Sort.descending(CommandVersionsKeys.createdAt))
                           .asList());
  }

  private PageResponse<CommandEntity> listCommandEntity(PageRequest<CommandEntity> pageRequest) {
    return wingsPersistence.query(CommandEntity.class, pageRequest, excludeAuthority);
  }

  private CommandEntity getCommandEntity(String commandStoreId, String commandId) {
    return wingsPersistence.createQuery(CommandEntity.class)
        .filter(CommandEntityKeys.commandStoreId, commandStoreId)
        .filter(CommandEntityKeys.uuid, commandId)
        .get();
  }

  private CommandVersionEntity getCommandVersionEntity(String commandStoreId, String commandId, String version) {
    return wingsPersistence.createQuery(CommandVersionEntity.class)
        .filter(CommandVersionsKeys.commandStoreId, commandStoreId)
        .filter(CommandVersionsKeys.commandId, commandId)
        .filter(CommandVersionsKeys.version, version)
        .get();
  }

  private CommandDTOBuilder populateCommandDTO(CommandDTOBuilder commandDTOBuilder, CommandEntity commandEntity,
      CommandVersionEntity latestCommandVersionEntity, List<CommandVersionEntity> allVersionList) {
    if (commandEntity != null) {
      commandDTOBuilder.id(commandEntity.getUuid())
          .commandStoreId(commandEntity.getCommandStoreId())
          .type(commandEntity.getType())
          .name(commandEntity.getName())
          .description(commandEntity.getDescription())
          .category(commandEntity.getDescription())
          .imageUrl(commandEntity.getImageUrl())
          .latestVersion(latestCommandVersionEntity != null
                  ? populateCommandVersionDTO(EnrichedCommandVersionDTO.builder(), latestCommandVersionEntity).build()
                  : null)
          .versionList(emptyIfNull(allVersionList)
                           .stream()
                           .map(versionEntity
                               -> populateCommandVersionDTO(EnrichedCommandVersionDTO.builder(), versionEntity).build())
                           .collect(toList()));
    }
    return commandDTOBuilder;
  }

  private EnrichedCommandVersionDTOBuilder populateCommandVersionDTO(
      EnrichedCommandVersionDTOBuilder builder, CommandVersionEntity commandVersionEntity) {
    if (commandVersionEntity != null) {
      builder.commandId(commandVersionEntity.getCommandId())
          .commandStoreId(commandVersionEntity.getCommandStoreId())
          .description(commandVersionEntity.getDescription())
          .version(commandVersionEntity.getVersion())
          .yamlContent(commandVersionEntity.getYamlContent())
          .templateObject(commandVersionEntity.getTemplateObject())
          .variables(commandVersionEntity.getVariables());
    }
    return builder;
  }
}