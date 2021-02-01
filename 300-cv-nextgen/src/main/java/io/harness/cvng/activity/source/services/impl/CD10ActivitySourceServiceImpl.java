package io.harness.cvng.activity.source.services.impl;

import io.harness.cvng.activity.entities.ActivitySource.ActivitySourceKeys;
import io.harness.cvng.activity.entities.CD10ActivitySource;
import io.harness.cvng.activity.source.services.api.CD10ActivitySourceService;
import io.harness.cvng.beans.activity.ActivitySourceType;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CD10ActivitySourceServiceImpl implements CD10ActivitySourceService {
  @Inject private HPersistence hPersistence;

  @Override
  public String getNextGenEnvIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String appId, String envId) {
    CD10ActivitySource cd10ActivitySource = getCd10ActivitySource(accountId, orgIdentifier, projectIdentifier);
    Preconditions.checkNotNull(
        cd10ActivitySource, "No CD 1.0 mapping defined for projectIdentifier: %s", projectIdentifier);
    return cd10ActivitySource.getEnvMappings()
        .stream()
        .filter(cd10EnvMappingDTO
            -> cd10EnvMappingDTO.getAppId().equals(appId) && cd10EnvMappingDTO.getEnvId().equals(envId))
        .findAny()
        .orElseThrow(() -> {
          log.info(
              "No envId to envIdentifier mapping exists envId: {}, cd10ActivitySource: {}", envId, cd10ActivitySource);
          return new IllegalStateException("No envId to envIdentifier mapping exists");
        })
        .getEnvIdentifier();
  }

  @Override
  public String getNextGenServiceIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String appId, String serviceId) {
    CD10ActivitySource cd10ActivitySource = getCd10ActivitySource(accountId, orgIdentifier, projectIdentifier);
    Preconditions.checkNotNull(
        cd10ActivitySource, "No CD 1.0 mapping defined for projectIdentifier: %s", projectIdentifier);
    return cd10ActivitySource.getServiceMappings()
        .stream()
        .filter(cd10ServiceMappingDTO
            -> cd10ServiceMappingDTO.getAppId().equals(appId) && cd10ServiceMappingDTO.getServiceId().equals(serviceId))
        .findAny()
        .orElseThrow(() -> {
          log.info("No serviceId to serviceIdentifier mapping exists serviceId: {}, cd10ActivitySource: {}", serviceId,
              cd10ActivitySource);
          return new IllegalStateException("No serviceId to serviceIdentifier mapping exists");
        })
        .getServiceIdentifier();
  }

  private CD10ActivitySource getCd10ActivitySource(String accountId, String orgIdentifier, String projectIdentifier) {
    return hPersistence.createQuery(CD10ActivitySource.class)
        .filter(ActivitySourceKeys.accountId, accountId)
        .filter(ActivitySourceKeys.orgIdentifier, orgIdentifier)
        .filter(ActivitySourceKeys.projectIdentifier, projectIdentifier)
        .filter(ActivitySourceKeys.type, ActivitySourceType.HARNESS_CD10)
        .get();
  }
}
