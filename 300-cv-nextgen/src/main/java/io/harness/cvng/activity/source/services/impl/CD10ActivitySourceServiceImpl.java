package io.harness.cvng.activity.source.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.activity.entities.ActivitySource.ActivitySourceKeys;
import io.harness.cvng.activity.entities.CD10ActivitySource;
import io.harness.cvng.activity.source.services.api.CD10ActivitySourceService;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.beans.activity.ActivitySourceType;
import io.harness.cvng.beans.activity.cd10.CD10ActivitySourceDTO;
import io.harness.cvng.beans.activity.cd10.CD10EnvMappingDTO;
import io.harness.cvng.beans.activity.cd10.CD10ServiceMappingDTO;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CD10ActivitySourceServiceImpl implements CD10ActivitySourceService {
  @Inject private HPersistence hPersistence;

  @Override
  public ActivitySourceDTO get(String accountId, String projectIdentifier, String appId) {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(projectIdentifier);
    Preconditions.checkNotNull(appId);

    List<ActivitySource> activitySources = hPersistence.createQuery(ActivitySource.class)
                                               .filter(ActivitySourceKeys.accountId, accountId)
                                               .filter(ActivitySourceKeys.projectIdentifier, projectIdentifier)
                                               .filter(ActivitySourceKeys.type, ActivitySourceType.HARNESS_CD10)
                                               .asList();

    if (isEmpty(activitySources)) {
      log.info("No CD1.0 sources found for {}, {}, {}", accountId, projectIdentifier, appId);
      return null;
    }

    Preconditions.checkState(activitySources.size() == 1, "We cannot have more than one CD 1.0 source for a project");
    CD10ActivitySource cd10ActivitySource = (CD10ActivitySource) activitySources.get(0);

    Set<CD10EnvMappingDTO> envsForApp = Collections.emptySet();
    Set<CD10ServiceMappingDTO> servicesForApp = Collections.emptySet();

    envsForApp = cd10ActivitySource.getEnvMappings()
                     .stream()
                     .filter(mapping -> mapping.getAppId().equals(appId))
                     .collect(Collectors.toSet());
    cd10ActivitySource.setEnvMappings(envsForApp);

    servicesForApp = cd10ActivitySource.getServiceMappings()
                         .stream()
                         .filter(mapping -> mapping.getAppId().equals(appId))
                         .collect(Collectors.toSet());
    cd10ActivitySource.setServiceMappings(servicesForApp);

    return cd10ActivitySource.toDTO();
  }

  @Override
  public ActivitySourceDTO get(
      String accountId, String projectIdentifier, String appId, String envId, String serviceId) {
    Preconditions.checkNotNull(envId);
    Preconditions.checkNotNull(serviceId);

    CD10ActivitySourceDTO cd10ActivitySource = (CD10ActivitySourceDTO) get(accountId, projectIdentifier, appId);

    if (cd10ActivitySource == null) {
      return null;
    }
    Set<CD10EnvMappingDTO> envsForApp = Collections.emptySet();
    Set<CD10ServiceMappingDTO> servicesForApp = Collections.emptySet();

    envsForApp = cd10ActivitySource.getEnvMappings()
                     .stream()
                     .filter(mapping -> mapping.getEnvId().equals(envId))
                     .collect(Collectors.toSet());
    cd10ActivitySource.setEnvMappings(envsForApp);

    servicesForApp = cd10ActivitySource.getServiceMappings()
                         .stream()
                         .filter(mapping -> mapping.getServiceId().equals(serviceId))
                         .collect(Collectors.toSet());
    cd10ActivitySource.setServiceMappings(servicesForApp);

    return cd10ActivitySource;
  }
}
