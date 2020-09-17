package software.wings.service.intfc.instance;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.ManualSyncJob;
import software.wings.beans.infrastructure.instance.SyncStatus;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.service.intfc.ownership.OwnedByApplication;
import software.wings.service.intfc.ownership.OwnedByEnvironment;
import software.wings.service.intfc.ownership.OwnedByInfrastructureMapping;
import software.wings.service.intfc.ownership.OwnedByService;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;

/**
 * @author rktummala on 08/17/17
 */
public interface InstanceService
    extends OwnedByApplication, OwnedByService, OwnedByEnvironment, OwnedByInfrastructureMapping, OwnedByAccount {
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

  /**
   * Gets instance information.
   *
   * @param instanceId the instance id
   * @param includeDeleted include deleted instance
   * @return the infrastructure mapping
   */
  Instance get(String instanceId, boolean includeDeleted);

  /**
   * Updates the entity. If entity doesn't exist, it creates one.
   *
   * @param instance the instance
   * @return the instance
   */
  @ValidationGroups(Update.class) Instance saveOrUpdate(@Valid Instance instance);

  @ValidationGroups(Update.class) Instance update(@Valid Instance instance, @NotEmpty String oldInstanceId);

  /**
   * Deletes the instances with the given ids
   *
   * @param instanceIdSet
   * @return
   */
  boolean delete(Set<String> instanceIdSet);

  /**
   * Purge the instances which were deleted up-to a given timestamp
   *
   * @param timestamp   - exclusive
   */
  boolean purgeDeletedUpTo(Instant timestamp);

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
   * List.
   *
   * @param pageRequest the req
   * @return the page response
   */
  PageResponse<Instance> list(PageRequest<Instance> pageRequest);

  PageResponse<Instance> listInstancesNotRemovedFully(PageRequest<Instance> pageRequest);

  /**
   * @param appId            the app id
   * @param serviceId        the service id
   * @param envId            the env id
   * @param infraMappingId   the infra mapping id
   * @param infraMappingName the infra mapping name
   * @param timestamp        sync timestamp
   */
  void updateSyncSuccess(
      String appId, String serviceId, String envId, String infraMappingId, String infraMappingName, long timestamp);

  /**
   * @param appId            the app id
   * @param serviceId        the service id
   * @param envId            the env id
   * @param infraMappingId   the infra mapping id
   * @param infraMappingName the infra mapping name
   * @param timestamp        failure sync timestamp
   * @param errorMsg         failure reason
   * @return
   */
  boolean handleSyncFailure(String appId, String serviceId, String envId, String infraMappingId,
      String infraMappingName, long timestamp, String errorMsg);
  /**
   *
   * @param appId
   * @param serviceId
   * @param envId
   * @return
   */
  List<SyncStatus> getSyncStatus(String appId, String serviceId, String envId);

  void saveManualSyncJob(ManualSyncJob manualSyncJob);
  void deleteManualSyncJob(String appId, String manualSyncJobId);

  List<Boolean> getManualSyncJobsStatus(String accountId, Set<String> manualJobIdSet);

  List<Instance> getInstancesForAppAndInframapping(String appId, String infraMappingId);

  long getInstanceCount(String appId, String infraMappingId);
}
