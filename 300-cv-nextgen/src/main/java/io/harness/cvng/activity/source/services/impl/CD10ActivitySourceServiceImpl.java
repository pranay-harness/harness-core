package io.harness.cvng.activity.source.services.impl;

import io.harness.cvng.activity.beans.Cd10ValidateMappingParams;
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
    return getNextGenEnvIdentifier(appId, envId, cd10ActivitySource);
  }

  private String getNextGenEnvIdentifier(String appId, String envId, CD10ActivitySource cd10ActivitySource) {
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
    return getNextGenServiceIdentifier(appId, serviceId, cd10ActivitySource);
  }

  private String getNextGenServiceIdentifier(String appId, String serviceId, CD10ActivitySource cd10ActivitySource) {
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

  @Override
  public void validateMapping(Cd10ValidateMappingParams cd10ValidateMappingParams) {
    CD10ActivitySource cd10ActivitySource =
        getCd10ActivitySource(cd10ValidateMappingParams.getAccountId(), cd10ValidateMappingParams.getOrgIdentifier(),
            cd10ValidateMappingParams.getProjectIdentifier(), cd10ValidateMappingParams.getActivitySourceIdentifier());
    String nextgenServiceIdentifier = getNextGenServiceIdentifier(
        cd10ValidateMappingParams.getCd10AppId(), cd10ValidateMappingParams.getCd10ServiceId(), cd10ActivitySource);
    String nextgenEnvIdentifier = getNextGenEnvIdentifier(
        cd10ValidateMappingParams.getCd10AppId(), cd10ValidateMappingParams.getCd10EnvId(), cd10ActivitySource);
    if (!cd10ValidateMappingParams.getServiceIdentifier().isRuntimeParam()
        && !nextgenServiceIdentifier.equals(cd10ValidateMappingParams.getServiceIdentifier().getValue())) {
      throw new IllegalStateException("Next gen Service identifier does not match CD 1.0 service mapping");
    }
    if (!cd10ValidateMappingParams.getEnvironmentIdentifier().isRuntimeParam()
        && !nextgenEnvIdentifier.equals(cd10ValidateMappingParams.getEnvironmentIdentifier().getValue())) {
      throw new IllegalStateException("Next gen env identifier does not match CD 1.0 env mappings");
    }
  }

  private CD10ActivitySource getCd10ActivitySource(String accountId, String orgIdentifier, String projectIdentifier) {
    return hPersistence.createQuery(CD10ActivitySource.class)
        .filter(ActivitySourceKeys.accountId, accountId)
        .filter(ActivitySourceKeys.orgIdentifier, orgIdentifier)
        .filter(ActivitySourceKeys.projectIdentifier, projectIdentifier)
        .filter(ActivitySourceKeys.type, ActivitySourceType.HARNESS_CD10)
        .get();
  }

  private CD10ActivitySource getCd10ActivitySource(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    return hPersistence.createQuery(CD10ActivitySource.class)
        .filter(ActivitySourceKeys.accountId, accountId)
        .filter(ActivitySourceKeys.orgIdentifier, orgIdentifier)
        .filter(ActivitySourceKeys.projectIdentifier, projectIdentifier)
        .filter(ActivitySourceKeys.identifier, identifier)
        .filter(ActivitySourceKeys.type, ActivitySourceType.HARNESS_CD10)
        .get();
  }
}
