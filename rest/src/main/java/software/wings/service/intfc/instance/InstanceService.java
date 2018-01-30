package software.wings.service.intfc.instance;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByApplication;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;

/**
 * @author rktummala on 08/17/17
 */
public interface InstanceService extends OwnedByApplication {
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
   *
   * @param instances instance entities
   * @return list of updated instances
   */
  List<Instance> saveOrUpdate(List<Instance> instances);

  List<Instance> save(List<Instance> instances);

  List<Instance> update(List<Instance> instances);

  /**
   * Gets instance information.
   *
   * @param instanceId the instance id
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
   *
   * @param instanceId the instance id
   */
  boolean delete(String instanceId);

  /**
   * Deletes the instances with the given ids
   *
   * @param instanceIdSet
   * @return
   */
  boolean delete(Set<String> instanceIdSet);

  /**
   * Handles save or update of container related instances.
   * Stale ones are also deleted.
   *
   * @param instanceType
   * @param containerSvcNameNoRevision
   * @param instanceList
   * @param appId
   */
  void saveOrUpdateContainerInstances(
      InstanceType instanceType, String containerSvcNameNoRevision, List<Instance> instanceList, String appId);

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
   *
   * @param containerSvcNameNoRevision
   * @param appId
   * @return
   */
  List<ContainerDeploymentInfo> getContainerDeploymentInfoList(String containerSvcNameNoRevision, String appId);

  /**
   * Get the container service names (taskDefinitionName for ECS and replicationControllerName for Kubernetes) of all
   * the container services that belong to the same family (containerSvcNameNoRevision) for the given app.
   *
   * @param containerSvcNameNoRevision
   * @param appId
   * @return
   */
  List<String> getContainerServiceNames(String containerSvcNameNoRevision, String appId);

  /**
   * Get the least recently synced container family (all container deployments with the same containerSvcNameNoRevision)
   * info from db. This is done so that no container family is starved from update. Each time the sync job comes up, it
   * picks up a batch of least visited families and updates the instances.
   *
   * @param appId
   * @param lastSyncTimestamp
   * @return
   */
  Set<String> getLeastRecentSyncedContainerDeployments(String appId, long lastSyncTimestamp);

  /**
   * Deletes the container deployments that have no active instances on the container server (ECS or Kubernetes).
   *
   * @param containerSvcNameSetToBeDeleted
   * @param instanceType
   * @param appId
   */
  void deleteContainerDeploymentInfoAndInstances(
      Set<String> containerSvcNameSetToBeDeleted, InstanceType instanceType, String appId);

  void deleteInstancesOfAutoScalingGroups(List<String> autoScalingGroupList, String appId);

  /**
   * List.
   *
   * @param pageRequest the req
   * @return the page response
   */
  PageResponse<Instance> list(PageRequest<Instance> pageRequest);
}
