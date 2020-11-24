package software.wings.beans;

import static java.lang.String.format;

import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.HelmVersion;

import software.wings.helpers.ext.helm.HelmCommandTemplateFactory.HelmCliCommandType;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Getter;

public final class HelmCommandFlagConstants {
  @Getter
  public enum HelmSubCommand {
    INSTALL(ImmutableSet.of(HelmCliCommandType.INSTALL.name()), "install"),
    UPGRADE(ImmutableSet.of(HelmCliCommandType.UPGRADE.name()), "upgrade"),
    ROLLBACK(ImmutableSet.of(HelmCliCommandType.ROLLBACK.name()), "rollback"),
    HISTORY(ImmutableSet.of(HelmCliCommandType.RELEASE_HISTORY.name()), "hist"),
    DELETE(ImmutableSet.of(HelmCliCommandType.DELETE_RELEASE.name()), "delete"),
    UNINSTALL(ImmutableSet.of(HelmCliCommandType.DELETE_RELEASE.name()), "uninstall"),
    LIST(ImmutableSet.of(HelmCliCommandType.LIST_RELEASE.name()), "list"),
    VERSION(ImmutableSet.of(HelmCliCommandType.VERSION.name()), "version"),
    PULL(ImmutableSet.of(HelmCliCommandType.FETCH.name()), "pull"),
    FETCH(ImmutableSet.of(HelmCliCommandType.FETCH.name()), "fetch"),
    TEMPLATE(
        ImmutableSet.of(HelmCliCommandType.RENDER_CHART.name(), HelmCliCommandType.RENDER_SPECIFIC_CHART_FILE.name()),
        "template");

    private final Set<String> commandTypes;
    private final String description;

    HelmSubCommand(Set<String> commandTypes, String description) {
      this.commandTypes = commandTypes;
      this.description = description;
    }
  }

  public static Set<HelmSubCommand> getHelmSubCommands(@NotNull HelmVersion version) {
    switch (version) {
      case V2:
        return ImmutableSet.of(HelmSubCommand.INSTALL, HelmSubCommand.UPGRADE, HelmSubCommand.ROLLBACK,
            HelmSubCommand.HISTORY, HelmSubCommand.DELETE, HelmSubCommand.LIST, HelmSubCommand.VERSION,
            HelmSubCommand.FETCH, HelmSubCommand.TEMPLATE);
      case V3:
        return ImmutableSet.of(HelmSubCommand.INSTALL, HelmSubCommand.UPGRADE, HelmSubCommand.ROLLBACK,
            HelmSubCommand.HISTORY, HelmSubCommand.UNINSTALL, HelmSubCommand.LIST, HelmSubCommand.VERSION,
            HelmSubCommand.PULL, HelmSubCommand.TEMPLATE);
      default:
        throw new InvalidRequestException(format("Version [%s] is not supported", version));
    }
  }
}
