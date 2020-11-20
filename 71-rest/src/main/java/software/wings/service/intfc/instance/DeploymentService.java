package software.wings.service.intfc.instance;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentSummary;
import software.wings.service.intfc.ownership.OwnedByApplication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;

public interface DeploymentService extends OwnedByApplication {
  /**
   * Save instance information.
   *
   * @param instance the instance
   * @return the instance
   */
  @ValidationGroups(Create.class) DeploymentSummary save(@Valid DeploymentSummary instance);

  /**
   * Gets instance information.
   */
  DeploymentSummary get(String deploymentSummaryId);

  /**
   * Deletes the instances with the given ids
   */
  boolean delete(Set<String> deploymentSummaryIdSet);

  PageResponse<DeploymentSummary> list(PageRequest<DeploymentSummary> pageRequest);

  Optional<DeploymentSummary> get(@Valid DeploymentSummary deploymentSummary);
  // DeploymentSummary saveOrUpdate(@Valid DeploymentSummary deploymentSummary);

  Optional<DeploymentSummary> getWithAccountId(DeploymentSummary deploymentSummary);

  Optional<DeploymentSummary> getWithInfraMappingId(String accountId, String infraMappingId);

  List<DeploymentSummary> getDeploymentSummary(String accountId, String offset, Instant startTime, Instant endTime);
}
