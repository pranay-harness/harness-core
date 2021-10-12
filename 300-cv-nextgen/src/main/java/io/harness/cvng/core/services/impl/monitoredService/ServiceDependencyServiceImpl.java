package io.harness.cvng.core.services.impl.monitoredService;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.ServiceDependencyDTO;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.entities.ServiceDependency;
import io.harness.cvng.core.entities.ServiceDependency.Key;
import io.harness.cvng.core.entities.ServiceDependency.ServiceDependencyKeys;
import io.harness.cvng.core.services.api.monitoredService.ServiceDependencyService;
import io.harness.persistence.HPersistence;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
@OwnedBy(CV)
public class ServiceDependencyServiceImpl implements ServiceDependencyService {
  @Inject private HPersistence hPersistence;

  @Override
  public void updateDependencies(ProjectParams projectParams, String toMonitoredServiceIdentifier,
      Set<ServiceDependencyDTO> fromMonitoredServiceIdentifiers) {
    List<ServiceDependency> dependencies = new ArrayList<>();
    fromMonitoredServiceIdentifiers.forEach(fromServiceIdentifier -> {
      dependencies.add(ServiceDependency.builder()
                           .accountId(projectParams.getAccountIdentifier())
                           .orgIdentifier(projectParams.getOrgIdentifier())
                           .projectIdentifier(projectParams.getProjectIdentifier())
                           .fromMonitoredServiceIdentifier(fromServiceIdentifier.getMonitoredServiceIdentifier())
                           .toMonitoredServiceIdentifier(toMonitoredServiceIdentifier)
                           .serviceDependencyMetadata(fromServiceIdentifier.getDependencyMetadata())
                           .build());
    });
    List<ServiceDependency> oldDependencies =
        hPersistence.createQuery(ServiceDependency.class)
            .filter(ServiceDependencyKeys.accountId, projectParams.getAccountIdentifier())
            .filter(ServiceDependencyKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(ServiceDependencyKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .filter(ServiceDependencyKeys.toMonitoredServiceIdentifier, toMonitoredServiceIdentifier)
            .asList();
    executeDBOperations(dependencies, oldDependencies);
  }

  private void executeDBOperations(List<ServiceDependency> newDependencies, List<ServiceDependency> oldDependencies) {
    Map<Key, ServiceDependency> newDependencyMap =
        newDependencies.stream().collect(Collectors.toMap(ServiceDependency::getKey, x -> x));
    Map<Key, ServiceDependency> oldDependencyMap =
        oldDependencies.stream().collect(Collectors.toMap(ServiceDependency::getKey, x -> x));

    Set<Key> deleteKeys = Sets.difference(oldDependencyMap.keySet(), newDependencyMap.keySet());
    deleteKeys.forEach(
        deleteKey -> hPersistence.delete(ServiceDependency.class, oldDependencyMap.get(deleteKey).getUuid()));

    Set<Key> createKeys = Sets.difference(newDependencyMap.keySet(), oldDependencyMap.keySet());
    List<ServiceDependency> createDependencies =
        createKeys.stream().map(newDependencyMap::get).collect(Collectors.toList());
    hPersistence.save(createDependencies);
  }

  @Override
  public void deleteDependenciesForService(ProjectParams projectParams, String monitoredServiceIdentifier) {
    Query<ServiceDependency> toServiceQuery =
        hPersistence.createQuery(ServiceDependency.class)
            .filter(ServiceDependencyKeys.accountId, projectParams.getAccountIdentifier())
            .filter(ServiceDependencyKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(ServiceDependencyKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .filter(ServiceDependencyKeys.toMonitoredServiceIdentifier, monitoredServiceIdentifier);
    hPersistence.delete(toServiceQuery);

    Query<ServiceDependency> fromServiceQuery =
        hPersistence.createQuery(ServiceDependency.class)
            .filter(ServiceDependencyKeys.accountId, projectParams.getAccountIdentifier())
            .filter(ServiceDependencyKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(ServiceDependencyKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .filter(ServiceDependencyKeys.fromMonitoredServiceIdentifier, monitoredServiceIdentifier);
    hPersistence.delete(fromServiceQuery);
  }

  @Override
  public Set<ServiceDependencyDTO> getDependentServicesForMonitoredService(
      ProjectParams projectParams, String monitoredServiceIdentifier) {
    Query<ServiceDependency> query =
        hPersistence.createQuery(ServiceDependency.class)
            .filter(ServiceDependencyKeys.accountId, projectParams.getAccountIdentifier())
            .filter(ServiceDependencyKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(ServiceDependencyKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .filter(ServiceDependencyKeys.toMonitoredServiceIdentifier, monitoredServiceIdentifier);
    List<ServiceDependency> dependencies = query.asList();
    return dependencies.stream()
        .map(d
            -> ServiceDependencyDTO.builder()
                   .monitoredServiceIdentifier(d.getFromMonitoredServiceIdentifier())
                   .dependencyMetadata(d.getServiceDependencyMetadata())
                   .build())
        .collect(Collectors.toSet());
  }

  @Override
  public Set<ServiceDependencyDTO> getDependentServicesToMonitoredService(
      @NonNull ProjectParams projectParams, String monitoredServiceIdentifier) {
    Query<ServiceDependency> query =
        hPersistence.createQuery(ServiceDependency.class)
            .filter(ServiceDependencyKeys.accountId, projectParams.getAccountIdentifier())
            .filter(ServiceDependencyKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(ServiceDependencyKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .filter(ServiceDependencyKeys.fromMonitoredServiceIdentifier, monitoredServiceIdentifier);
    List<ServiceDependency> dependencies = query.asList();
    return dependencies.stream()
        .map(
            d -> ServiceDependencyDTO.builder().monitoredServiceIdentifier(d.getToMonitoredServiceIdentifier()).build())
        .collect(Collectors.toSet());
  }

  @Override
  public List<ServiceDependency> getServiceDependencies(
      @NonNull ProjectParams projectParams, @Nullable List<String> monitoredServiceIdentifiers) {
    Query<ServiceDependency> query =
        hPersistence.createQuery(ServiceDependency.class)
            .filter(ServiceDependencyKeys.accountId, projectParams.getAccountIdentifier())
            .filter(ServiceDependencyKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(ServiceDependencyKeys.projectIdentifier, projectParams.getProjectIdentifier());
    if (monitoredServiceIdentifiers != null) {
      query.field(ServiceDependencyKeys.toMonitoredServiceIdentifier).in(monitoredServiceIdentifiers);
    }

    return query.asList();
  }

  @Override
  public void deleteByProjectIdentifier(
      Class<ServiceDependency> clazz, String accountId, String orgIdentifier, String projectIdentifier) {
    Query<ServiceDependency> deleteQuery = hPersistence.createQuery(ServiceDependency.class)
                                               .filter(ServiceDependencyKeys.accountId, accountId)
                                               .filter(ServiceDependencyKeys.orgIdentifier, orgIdentifier)
                                               .filter(ServiceDependencyKeys.projectIdentifier, projectIdentifier);
    hPersistence.deleteOnServer(deleteQuery);
  }

  @Override
  public void deleteByOrgIdentifier(Class<ServiceDependency> clazz, String accountId, String orgIdentifier) {
    Query<ServiceDependency> deleteQuery = hPersistence.createQuery(ServiceDependency.class)
                                               .filter(ServiceDependencyKeys.accountId, accountId)
                                               .filter(ServiceDependencyKeys.orgIdentifier, orgIdentifier);
    hPersistence.deleteOnServer(deleteQuery);
  }

  @Override
  public void deleteByAccountIdentifier(Class<ServiceDependency> clazz, String accountId) {
    Query<ServiceDependency> deleteQuery =
        hPersistence.createQuery(ServiceDependency.class).filter(ServiceDependencyKeys.accountId, accountId);
    hPersistence.deleteOnServer(deleteQuery);
  }
}
