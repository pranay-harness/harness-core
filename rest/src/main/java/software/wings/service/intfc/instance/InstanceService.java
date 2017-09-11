package software.wings.service.intfc.instance;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.PhaseExecutionData;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.key.InstanceKey;
import software.wings.sm.ExecutionContext;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

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
   * Get instance information.
   *
   * @param instanceId  the instance id
   * @return the infrastructure mapping
   */
  Instance get(String instanceId);

  /**
   * Update the entity. If entity doesn't exist, it creates one.
   *
   * @param instance the instance
   * @return the instance
   */
  @ValidationGroups(Update.class) Instance saveOrUpdate(@Valid Instance instance);

  Instance update(@Valid Instance instance) throws Exception;

  /**
   * Delete the given instance.
   * @param instanceId the instance id
   */
  boolean delete(String instanceId);

  boolean delete(Set<String> instanceIdSet);

  /**
   * Builds the instances from the container deployment info and save the instances to the database.
   * @param containerDeploymentInfo
   * @param containerInfoList
   */
  void buildAndSaveInstances(ContainerDeploymentInfo containerDeploymentInfo, List<ContainerInfo> containerInfoList);
}
