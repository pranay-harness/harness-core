package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Environment;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.validation.Valid;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/1/16.
 */
public interface EnvironmentService {
  /**
   * List.
   *
   * @param request the request
   * @return the page response
   */
  PageResponse<Environment> list(PageRequest<Environment> request, boolean withSummary);

  /**
   * Gets the.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the environment
   */
  Environment get(@NotEmpty String appId, @NotEmpty String envId);

  /**
   * Save.
   *
   * @param environment the environment
   * @return the environment
   */
  @ValidationGroups(Create.class) Environment save(@Valid Environment environment);

  /**
   * Update.
   *
   * @param environment the environment
   * @return the environment
   */
  @ValidationGroups(Update.class) Environment update(@Valid Environment environment);

  /**
   * Delete.
   *
   * @param appId the app id
   * @param envId the env id
   */
  void delete(@NotEmpty String appId, @NotEmpty String envId);

  /**
   * Delete by app id.
   *
   * @param appId the app id
   */
  void deleteByApp(@NotEmpty String appId);

  /**
   * Create default environments.
   *
   * @param appId the app id
   */
  void createDefaultEnvironments(@NotEmpty String appId);

  /**
   * Gets env by app.
   *
   * @param appId the app id
   * @return the env by app
   */
  List<Environment> getEnvByApp(@NotEmpty String appId);
}
