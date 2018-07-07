package software.wings.service.impl.command;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.CommandCategory.CommandUnit;
import static software.wings.beans.CommandCategory.Type.COMMANDS;
import static software.wings.beans.CommandCategory.Type.COPY;
import static software.wings.beans.CommandCategory.Type.SCRIPTS;
import static software.wings.beans.CommandCategory.Type.VERIFICATIONS;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.CommandCategory;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ServiceCommand;
import software.wings.dl.WingsPersistence;
import software.wings.stencils.StencilCategory;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class CommandHelper {
  @Inject WingsPersistence wingsPersistence;

  /**
   * Get Command categories of service and service command
   * @param appId
   * @param serviceId
   * @param commandName
   * @return List of Command Categories
   */
  public List<CommandCategory> getCommandCategories(
      @NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String commandName) {
    List<ServiceCommand> serviceCommands = wingsPersistence.createQuery(ServiceCommand.class)
                                               .project("name", true)
                                               .filter("appId", appId)
                                               .filter("serviceId", serviceId)
                                               .asList();

    List<CommandUnit> commands =
        serviceCommands.stream()
            .filter(serviceCommand -> !commandName.equals(serviceCommand.getName()))
            .map(serviceCommand
                -> CommandUnit.builder().name(serviceCommand.getName()).type(CommandUnitType.COMMAND).build())
            .collect(toList());

    return getCommandCategories(commands);
  }

  public static List<CommandCategory> getCommandCategories(List<CommandUnit> commands) {
    List<CommandCategory> categories = new ArrayList<>();
    List<CommandUnit> scripts = new ArrayList<>();
    List<CommandUnit> copyUnits = new ArrayList<>();
    List<CommandUnit> verifications = new ArrayList<>();

    for (CommandUnitType commandUnitType : CommandUnitType.values()) {
      StencilCategory stencilCategory = commandUnitType.getStencilCategory();
      CommandUnit commandUnitItem = CommandUnit.builder().name(commandUnitType.getName()).type(commandUnitType).build();
      if (StencilCategory.SCRIPTS.equals(stencilCategory)) {
        scripts.add(commandUnitItem);
      } else if (StencilCategory.COPY.equals(stencilCategory)) {
        copyUnits.add(commandUnitItem);
      } else if (StencilCategory.VERIFICATIONS.equals(stencilCategory)) {
        verifications.add(commandUnitItem);
      }
    }

    if (isNotEmpty(scripts)) {
      categories.add(
          CommandCategory.builder().type(SCRIPTS).displayName(SCRIPTS.getDisplayName()).commandUnits(scripts).build());
    }

    if (isNotEmpty(copyUnits)) {
      categories.add(
          CommandCategory.builder().type(COPY).displayName(COPY.getDisplayName()).commandUnits(copyUnits).build());
    }

    if (isNotEmpty(commands)) {
      categories.add(CommandCategory.builder()
                         .type(COMMANDS)
                         .displayName(COMMANDS.getDisplayName())
                         .commandUnits(commands)
                         .build());
    }

    categories.add(CommandCategory.builder()
                       .type(VERIFICATIONS)
                       .displayName(VERIFICATIONS.getDisplayName())
                       .commandUnits(verifications)
                       .build());

    return categories;
  }
}
