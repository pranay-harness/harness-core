package software.wings.helpers.ext.helm;

import static java.lang.String.format;
import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion.V2;

import io.harness.exception.InvalidRequestException;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;

public final class HelmCommandTemplateFactory {
  public enum HelmCliCommandType {
    INSTALL,
    UPGRADE,
    ROLLBACK,
    RELEASE_HISTORY,
    DELETE_RELEASE,
    LIST_RELEASE,
    REPO_ADD,
    REPO_UPDATE,
    REPO_LIST,
    SEARCH_REPO,
    VERSION,
    REPO_ADD_CHART_MEUSEUM,
    REPO_ADD_HTTP,
    FETCH,
    REPO_REMOVE,
    INIT,
    RENDER_CHART,
    RENDER_SPECIFIC_CHART_FILE;
  }

  /*
  default to v2 if version not set
   */
  public static String getHelmCommandTemplate(HelmCliCommandType commandType, HelmVersion version) {
    if (version == null) {
      version = V2;
    }
    switch (commandType) {
      case VERSION:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_VERSION_COMMAND_TEMPLATE;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_VERSION_COMMAND_TEMPLATE;
        }
      case INIT:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_INIT_COMMAND;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_INIT_COMMAND;
        }
      case INSTALL:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_INSTALL_COMMAND_TEMPLATE;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_INSTALL_COMMAND_TEMPLATE;
        }
      case UPGRADE:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_UPGRADE_COMMAND_TEMPLATE;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_UPGRADE_COMMAND_TEMPLATE;
        }
      case ROLLBACK:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_ROLLBACK_COMMAND_TEMPLATE;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_ROLLBACK_COMMAND_TEMPLATE;
        }
      case REPO_ADD:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_ADD_REPO_COMMAND_TEMPLATE;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_ADD_REPO_COMMAND_TEMPLATE;
        }
      case REPO_UPDATE:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_REPO_UPDATE_COMMAND_TEMPLATE;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_REPO_UPDATE_COMMAND_TEMPLATE;
        }
      case REPO_LIST:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_REPO_LIST_COMMAND_TEMPLATE;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_REPO_LIST_COMMAND_TEMPLATE;
        }

      case RELEASE_HISTORY:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_RELEASE_HIST_COMMAND_TEMPLATE;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_RELEASE_HIST_COMMAND_TEMPLATE;
        }
      case LIST_RELEASE:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_LIST_RELEASE_COMMAND_TEMPLATE;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_LIST_RELEASE_COMMAND_TEMPLATE;
        }
      case DELETE_RELEASE:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_DELETE_RELEASE_TEMPLATE;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_DELETE_RELEASE_TEMPLATE;
        }
      case SEARCH_REPO:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_SEARCH_COMMAND_TEMPLATE;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_SEARCH_COMMAND_TEMPLATE;
        }
      case REPO_REMOVE:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_REPO_REMOVE_COMMAND;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_REPO_REMOVE_COMMAND;
        }
      case REPO_ADD_HTTP:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_REPO_ADD_COMMAND_FOR_HTTP;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_REPO_ADD_COMMAND_FOR_HTTP;
        }
      case REPO_ADD_CHART_MEUSEUM:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_REPO_ADD_COMMAND_FOR_CHART_MUSEUM;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_REPO_ADD_COMMAND_FOR_CHART_MUSEUM;
        }
      case FETCH:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_FETCH_COMMAND;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_FETCH_COMMAND;
        }
      case RENDER_CHART:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_TEMPLATE_COMMAND_FOR_KUBERNETES_TEMPLATE;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_TEMPLATE_COMMAND_FOR_KUBERNETES_TEMPLATE;
        }
      case RENDER_SPECIFIC_CHART_FILE:
        switch (version) {
          case V3:
            return HelmConstants.V3Commands.HELM_RENDER_SPECIFIC_TEMPLATE;
          case V2:
          default:
            return HelmConstants.V2Commands.HELM_RENDER_SPECIFIC_TEMPLATE;
        }
      default:
        throw new InvalidRequestException(format("Command Type [%s] is not supported", commandType.toString()));
    }
  }
}
