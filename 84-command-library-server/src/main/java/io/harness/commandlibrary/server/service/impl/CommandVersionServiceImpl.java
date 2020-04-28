package io.harness.commandlibrary.server.service.impl;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.commandlibrary.server.beans.CommandArchiveContext;
import io.harness.commandlibrary.server.beans.CommandManifest;
import io.harness.commandlibrary.server.service.intfc.CommandArchiveHandler;
import io.harness.commandlibrary.server.service.intfc.CommandService;
import io.harness.commandlibrary.server.service.intfc.CommandVersionService;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnsupportedOperationException;
import io.harness.persistence.HQuery;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.commandlibrary.CommandVersionEntity;
import software.wings.dl.WingsPersistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Singleton
@Slf4j
public class CommandVersionServiceImpl implements CommandVersionService {
  public static final Pattern VESRION_REGEX_PATTERN = Pattern.compile("^(\\d+\\.)?(\\d+\\.)?(\\*|\\d+)$");
  private final WingsPersistence wingsPersistence;
  private final Set<CommandArchiveHandler> commandArchiveHandlers;
  private final CommandService commandService;
  private final CommandVersionService commandVersionService;
  @Inject
  public CommandVersionServiceImpl(WingsPersistence wingsPersistence, Set<CommandArchiveHandler> commandArchiveHandlers,
      CommandService commandService, CommandVersionService commandVersionService) {
    this.wingsPersistence = wingsPersistence;
    this.commandArchiveHandlers = commandArchiveHandlers;
    this.commandService = commandService;
    this.commandVersionService = commandVersionService;
  }

  @Override
  public Optional<CommandVersionEntity> getCommandVersionEntity(
      String commandStoreName, String commandName, String version) {
    return ofNullable(wingsPersistence.createQuery(CommandVersionEntity.class)
                          .filter(CommandVersionEntity.CommandVersionsKeys.commandStoreName, commandStoreName)
                          .filter(CommandVersionEntity.CommandVersionsKeys.commandName, commandName)
                          .filter(CommandVersionEntity.CommandVersionsKeys.version, version)
                          .get());
  }

  @Override
  public List<CommandVersionEntity> getAllVersionEntitiesForCommand(String commandStoreName, String commandName) {
    return emptyIfNull(wingsPersistence.createQuery(CommandVersionEntity.class, HQuery.excludeAuthority)
                           .filter(CommandVersionEntity.CommandVersionsKeys.commandStoreName, commandStoreName)
                           .filter(CommandVersionEntity.CommandVersionsKeys.commandName, commandName)
                           .order(Sort.descending(CommandVersionEntity.CommandVersionsKeys.createdAt))
                           .asList());
  }

  @Override
  public String save(CommandVersionEntity commandVersionEntity) {
    return wingsPersistence.save(commandVersionEntity);
  }

  @Override
  public Optional<CommandVersionEntity> getEntityById(String commandVersionId) {
    return Optional.ofNullable(wingsPersistence.get(CommandVersionEntity.class, commandVersionId));
  }

  @Override
  public String createNewVersionFromArchive(CommandArchiveContext commandArchiveContext) {
    validateManifest(commandArchiveContext);
    final CommandArchiveHandler archiveHandler = getArchiveHandler(commandArchiveContext);
    return archiveHandler.createNewCommandVersion(commandArchiveContext);
  }

  private CommandArchiveHandler getArchiveHandler(CommandArchiveContext commandArchiveContext) {
    return commandArchiveHandlers.stream()
        .filter(commandArchiveHandler -> commandArchiveHandler.supports(commandArchiveContext))
        .findFirst()
        .orElseThrow(() -> new UnsupportedOperationException("archive handler could not be found"));
  }

  private void validateManifest(CommandArchiveContext commandArchiveContext) {
    final CommandManifest commandManifest = commandArchiveContext.getCommandManifest();
    final String type = commandManifest.getType();
    final String commandStoreName = commandArchiveContext.getCommandStoreName();
    validateVersionDoesNotExist(commandStoreName, commandManifest);
    validateCommandType(type, commandStoreName);
    validateVersion(commandManifest);
  }

  private void validateVersion(CommandManifest commandManifest) {
    final String version = commandManifest.getVersion();
    if (!VESRION_REGEX_PATTERN.matcher(version).matches()) {
      throw new InvalidRequestException(format("version [%s] does not meet the required syntax eg 0.1.1", version));
    }
  }

  private void validateVersionDoesNotExist(String commandStoreName, CommandManifest manifest) {
    final Optional<CommandVersionEntity> commandVersionEntityOpt =
        commandVersionService.getCommandVersionEntity(commandStoreName, manifest.getName(), manifest.getVersion());
    if (commandVersionEntityOpt.isPresent()) {
      throw new InvalidRequestException(
          format("Version already exist with details store = [%s], command =[%s], version =[%s]", commandStoreName,
              manifest.getName(), manifest.getVersion()));
    }
  }

  private void validateCommandType(String type, String commandStoreName) {
    if (!commandService.isCommandTypeSupported(commandStoreName, type)) {
      throw new InvalidRequestException(format("command type =[%s] not supported", type));
    }
  }
}
