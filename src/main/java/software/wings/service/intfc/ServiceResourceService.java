package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Command;
import software.wings.beans.Graph;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.Map;
import javax.validation.Valid;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 3/28/16.
 */
public interface ServiceResourceService {
  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Service> list(PageRequest<Service> pageRequest);

  /**
   * Save.
   *
   * @param service the service
   * @return the service
   */
  @ValidationGroups(Create.class) Service save(@Valid Service service);

  /**
   * Update.
   *
   * @param service the service
   * @return the service
   */
  @ValidationGroups(Update.class) Service update(@Valid Service service);

  /**
   * Gets the.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the service
   */
  Service get(@NotEmpty String appId, @NotEmpty String serviceId);

  /**
   * Delete.
   *
   * @param appId     the app id
   * @param serviceId the service id
   */
  void delete(@NotEmpty String appId, @NotEmpty String serviceId);

  /**
   * Adds the command.
   *
   * @param appId        the app id
   * @param serviceId    the service id
   * @param commandGraph the command graph
   * @return the service
   */
  Service addCommand(@NotEmpty String appId, @NotEmpty String serviceId, Graph commandGraph);

  /**
   * Update command service.
   *
   * @param appId        the app id
   * @param serviceId    the service id
   * @param commandGraph the command graph
   * @return the service
   */
  Service updateCommand(String appId, String serviceId, Graph commandGraph);

  /**
   * Delete command.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param commandName the command name
   * @return the service
   */
  Service deleteCommand(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String commandName);

  /**
   * Gets command by name.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param commandName the command name
   * @return the command by name
   */
  Command getCommandByName(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String commandName);

  /**
   * Gets command stencils.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the command stencils
   */
  Map<String, String> getCommandStencils(@NotEmpty String appId, @NotEmpty String serviceId);
}
