package io.harness.cvng.core.services.impl.monitoredService;

import static io.harness.annotations.dev.HarnessTeam.CV;
import static io.harness.ng.core.utils.NGUtils.validate;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.ServiceRef;
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
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
@OwnedBy(CV)
public class ServiceDependencyServiceImpl implements ServiceDependencyService {
  @Inject private HPersistence hPersistence;

  @Override
  public void createOrDelete(String accountId, String orgIdentifier, String projectIdentifier, String envIdentifier,
      Set<ServiceRef> fromServiceIdentifiers, String toServiceIdentifier) {
    List<ServiceDependency> dependencies = new ArrayList<>();
    fromServiceIdentifiers.forEach(fromServiceIdentifier -> {
      dependencies.add(ServiceDependency.builder()
                           .accountId(accountId)
                           .orgIdentifier(orgIdentifier)
                           .projectIdentifier(projectIdentifier)
                           .environmentIdentifier(envIdentifier)
                           .fromServiceIdentifier(fromServiceIdentifier.getServiceRef())
                           .toServiceIdentifier(toServiceIdentifier)
                           .build());
    });
    validate(dependencies);
    List<ServiceDependency> oldDependencies =
        hPersistence.createQuery(ServiceDependency.class)
            .filter(ServiceDependencyKeys.accountId, accountId)
            .filter(ServiceDependencyKeys.orgIdentifier, orgIdentifier)
            .filter(ServiceDependencyKeys.projectIdentifier, projectIdentifier)
            .filter(ServiceDependencyKeys.environmentIdentifier, envIdentifier)
            .filter(ServiceDependencyKeys.toServiceIdentifier, toServiceIdentifier)
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
  public void deleteDependenciesForService(String accountId, String orgIdentifier, String projectIdentifier,
      String envIdentifier, String serviceIdentifier) {
    Query<ServiceDependency> toServiceQuery = hPersistence.createQuery(ServiceDependency.class)
                                                  .filter(ServiceDependencyKeys.accountId, accountId)
                                                  .filter(ServiceDependencyKeys.orgIdentifier, orgIdentifier)
                                                  .filter(ServiceDependencyKeys.projectIdentifier, projectIdentifier)
                                                  .filter(ServiceDependencyKeys.environmentIdentifier, envIdentifier)
                                                  .filter(ServiceDependencyKeys.toServiceIdentifier, serviceIdentifier);
    hPersistence.deleteOnServer(toServiceQuery);
  }

  @Override
  public Set<ServiceRef> getDependentServicesForMonitoredService(String accountId, String orgIdentifier,
      String projectIdentifier, String envIdentifier, String serviceIdentifier) {
    List<ServiceDependency> dependencies = hPersistence.createQuery(ServiceDependency.class)
                                               .filter(ServiceDependencyKeys.accountId, accountId)
                                               .filter(ServiceDependencyKeys.orgIdentifier, orgIdentifier)
                                               .filter(ServiceDependencyKeys.projectIdentifier, projectIdentifier)
                                               .filter(ServiceDependencyKeys.environmentIdentifier, envIdentifier)
                                               .filter(ServiceDependencyKeys.toServiceIdentifier, serviceIdentifier)
                                               .asList();
    return dependencies.stream()
        .map(d -> ServiceRef.builder().serviceRef(d.getFromServiceIdentifier()).build())
        .collect(Collectors.toSet());
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
