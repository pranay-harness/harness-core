package software.wings.service.intfc.dashboardStats;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.infrastructure.Instance;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.validation.Valid;

/**
 * @author rktummala on 08/17/17
 */
public interface InstanceService {
  /**
   * Save instance information.
   *
   * @param instance the instance
   * @return the instance
   */
  @ValidationGroups(Create.class) Instance save(@Valid Instance instance);

  /**
   * Update the list of entities. If entity doesn't exist, it creates one.
   * This is not a batch update since morphia client doesn't support bulk writes in version 1.3.1.
   * @param instances instance entities
   * @return list of updated instances
   */
  List<Instance> saveOrUpdate(List<Instance> instances);

  /**
   * Get instance information.
   *
   * @param appId  the app id
   * @param instanceId  the instance id
   * @return the infrastructure mapping
   */
  Instance get(String appId, String instanceId);

  /**
   * Update the entity. If entity doesn't exist, it creates one.
   *
   * @param instance the instance
   * @return the instance
   */
  @ValidationGroups(Update.class) Instance saveOrUpdate(@Valid Instance instance);

  /**
   * Delete.
   * @param appId the application id
   * @param instanceId the instance id
   */
  boolean delete(String appId, String instanceId);
}
