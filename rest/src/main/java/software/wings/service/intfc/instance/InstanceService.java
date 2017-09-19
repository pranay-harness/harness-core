package software.wings.service.intfc.instance;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.Collection;
import java.util.List;
import java.util.Set;
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

  List<Instance> save(List<Instance> instances);

  List<Instance> update(List<Instance> instances);

  /**
   * Gets instance information.
   *
   * @param instanceId  the instance id
   * @return the infrastructure mapping
   */
  Instance get(String instanceId);

  /**
   * Updates the entity. If entity doesn't exist, it creates one.
   *
   * @param instance the instance
   * @return the instance
   */
  @ValidationGroups(Update.class) Instance saveOrUpdate(@Valid Instance instance);

  Instance update(@Valid Instance instance) throws Exception;

  /**
   * Deletes the given instance.
   * @param instanceId the instance id
   */
  boolean delete(String instanceId);

  /**
   * Deletes all the instances of an app
   * @param appId application id
   * @return
   */
  boolean deleteByApp(String appId);

  /**
   * Deletes the instances with the given ids
   * @param instanceIdSet
   * @return
   */
  boolean delete(Set<String> instanceIdSet);

  /**
   * Handles save or update of container related instances.
   * Stale ones are also deleted.
   * @param instanceType
   * @param containerSvcNameNoRevision
   * @param instanceList
   */
  void saveOrUpdateContainerInstances(
      InstanceType instanceType, String containerSvcNameNoRevision, List<Instance> instanceList);

  /**
   * @param containerSvcNameNoRevision
   * @param containerDeploymentInfoCollection
   * @param appId
   * @param instanceType
   * @param syncTimestamp
   */
  void saveOrUpdateContainerDeploymentInfo(String containerSvcNameNoRevision,
      Collection<ContainerDeploymentInfo> containerDeploymentInfoCollection, String appId, InstanceType instanceType,
      long syncTimestamp);

  /**
   * Get the container deployment info of all the container services that belong to the same family
   * containerSvcNameNoRevision for the given app.
   * @param containerSvcNameNoRevision
   * @param appId
   * @return
   */
  List<ContainerDeploymentInfo> getContainerDeploymentInfoList(String containerSvcNameNoRevision, String appId);

  List<String> getContainerServiceNames(String containerServiceNameWithoutRevision, String appId);

  Set<String> getLeastRecentVisitedContainerDeployments(String appId, long lastVisitedTimestamp);

  void deleteContainerDeploymentInfoAndInstances(
      Set<String> containerServiceNameSetToBeDeleted, InstanceType instanceType, String appId);
}
