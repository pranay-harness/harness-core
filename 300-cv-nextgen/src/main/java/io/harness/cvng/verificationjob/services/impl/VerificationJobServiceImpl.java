package io.harness.cvng.verificationjob.services.impl;

import static io.harness.cvng.CVConstants.DEFAULT_HEALTH_JOB_ID;
import static io.harness.cvng.CVConstants.DEFAULT_HEALTH_JOB_NAME;
import static io.harness.cvng.verificationjob.beans.VerificationJobType.HEALTH;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.services.api.CVEventService;
import io.harness.cvng.verificationjob.beans.HealthVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.RuntimeParameter.RuntimeParameterKeys;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobKeys;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.persistence.HPersistence;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

@Slf4j
public class VerificationJobServiceImpl implements VerificationJobService {
  @Inject private HPersistence hPersistence;
  @Inject private NextGenService nextGenService;
  @Inject private CVEventService cvEventService;

  @Override
  @Nullable
  public VerificationJobDTO getVerificationJobDTO(String accountId, String identifier) {
    VerificationJob verificationJob = getVerificationJob(accountId, identifier);
    if (verificationJob == null) {
      return null;
    }
    return verificationJob.getVerificationJobDTO();
  }

  @Override
  public void upsert(String accountId, VerificationJobDTO verificationJobDTO) {
    VerificationJob verificationJob = verificationJobDTO.getVerificationJob();
    verificationJob.setAccountId(accountId);
    VerificationJob stored = getVerificationJob(accountId, verificationJobDTO.getIdentifier());
    if (stored != null) {
      Preconditions.checkState(stored.getProjectIdentifier().equals(verificationJob.getProjectIdentifier()));
      Preconditions.checkState(stored.getOrgIdentifier().equals(verificationJob.getOrgIdentifier()));

      verificationJob.setUuid(stored.getUuid());
    }
    verificationJob.validate();
    // TODO: Keeping it simple for now. find a better way to save if more fields are added to verification Job. This can
    // potentially override them.
    save(verificationJob);
  }

  @Override
  public void save(VerificationJob verificationJob) {
    hPersistence.save(verificationJob);
    sendScopedCreateEvent(verificationJob);
  }

  private void sendScopedCreateEvent(VerificationJob verificationJob) {
    if (!verificationJob.getEnvIdentifierRuntimeParam().isRuntimeParam()) {
      cvEventService.sendVerificationJobEnvironmentCreateEvent(verificationJob);
    }
    if (!verificationJob.getServiceIdentifierRuntimeParam().isRuntimeParam()) {
      cvEventService.sendVerificationJobServiceCreateEvent(verificationJob);
    }
  }

  @Override
  public VerificationJob getVerificationJob(String accountId, String identifier) {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(identifier);
    return hPersistence.createQuery(VerificationJob.class)
        .filter(VerificationJobKeys.accountId, accountId)
        .filter(VerificationJobKeys.identifier, identifier)
        .get();
  }

  @Override
  public List<VerificationJob> getHealthVerificationJobs(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String envIdentifier, String serviceIdentifier) {
    Preconditions.checkNotNull(accountIdentifier);
    Preconditions.checkNotNull(orgIdentifier);
    Preconditions.checkNotNull(projectIdentifier);
    Preconditions.checkNotNull(envIdentifier);
    Preconditions.checkNotNull(serviceIdentifier);
    List<VerificationJob> specificHealthJobs =
        hPersistence.createQuery(VerificationJob.class)
            .filter(VerificationJobKeys.accountId, accountIdentifier)
            .filter(VerificationJobKeys.orgIdentifier, orgIdentifier)
            .filter(VerificationJobKeys.projectIdentifier, projectIdentifier)
            .filter(VerificationJobKeys.envIdentifier + "." + RuntimeParameterKeys.value, envIdentifier)
            .filter(VerificationJobKeys.serviceIdentifier + "." + RuntimeParameterKeys.value, serviceIdentifier)
            .filter(VerificationJobKeys.type, HEALTH)
            .asList();
    if (isEmpty(specificHealthJobs)) {
      VerificationJob defaultJob =
          getOrCreateDefaultHealthVerificationJob(accountIdentifier, orgIdentifier, projectIdentifier);
      defaultJob.setServiceIdentifier(serviceIdentifier, false);
      defaultJob.setEnvIdentifier(envIdentifier, false);
      return Arrays.asList(defaultJob);
    } else {
      return specificHealthJobs;
    }
  }

  @Override
  public VerificationJob get(String uuid) {
    Preconditions.checkNotNull(uuid);
    return hPersistence.get(VerificationJob.class, uuid);
  }

  @Override
  public VerificationJob getByUrl(String accountId, String verificationJobUrl) {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(verificationJobUrl);

    String identifier = getParamFromUrl(verificationJobUrl, VerificationJobKeys.identifier);
    String orgIdentifier = getParamFromUrl(verificationJobUrl, VerificationJobKeys.orgIdentifier);
    String projectIdentifier = getParamFromUrl(verificationJobUrl, VerificationJobKeys.projectIdentifier);
    Preconditions.checkNotNull(identifier);
    Preconditions.checkNotNull(orgIdentifier);
    Preconditions.checkNotNull(projectIdentifier);

    // TODO: Use the fully qualified method once it's available.
    return hPersistence.createQuery(VerificationJob.class)
        .filter(VerificationJobKeys.accountId, accountId)
        .filter(VerificationJobKeys.orgIdentifier, orgIdentifier)
        .filter(VerificationJobKeys.projectIdentifier, projectIdentifier)
        .filter(VerificationJobKeys.identifier, identifier)
        .get();
  }

  private String getParamFromUrl(String url, String paramName) {
    try {
      List<NameValuePair> queryParams = new URIBuilder(url).getQueryParams();
      return queryParams.stream()
          .filter(param -> param.getName().equalsIgnoreCase(paramName))
          .map(NameValuePair::getValue)
          .findFirst()
          .orElse(null);
    } catch (URISyntaxException ex) {
      log.error("Exception while parsing URL: " + url, ex);
      throw new IllegalStateException("Exception while parsing URL: " + url);
    }
  }

  @Override
  public void delete(String accountId, String identifier) {
    VerificationJob verificationJob = getVerificationJob(accountId, identifier);
    sendScopedDeleteEvent(verificationJob);
    hPersistence.delete(hPersistence.createQuery(VerificationJob.class)
                            .filter(VerificationJobKeys.accountId, accountId)
                            .filter(VerificationJobKeys.identifier, identifier));
  }

  private void sendScopedDeleteEvent(VerificationJob verificationJob) {
    if (!verificationJob.getEnvIdentifierRuntimeParam().isRuntimeParam()) {
      cvEventService.sendVerificationJobEnvironmentDeleteEvent(verificationJob);
    }
    if (!verificationJob.getServiceIdentifierRuntimeParam().isRuntimeParam()) {
      cvEventService.sendVerificationJobServiceDeleteEvent(verificationJob);
    }
  }

  @Override
  public PageResponse<VerificationJobDTO> list(
      String accountId, String projectId, String orgIdentifier, Integer offset, Integer pageSize, String filter) {
    List<VerificationJob> verificationJobs = verificationJobList(accountId, projectId, orgIdentifier);

    List<VerificationJobDTO> verificationJobList = new ArrayList<>();

    for (VerificationJob verificationJob : verificationJobs) {
      if (isEmpty(filter) || verificationJob.getJobName().toLowerCase().contains(filter.trim().toLowerCase())) {
        verificationJobList.add(verificationJob.getVerificationJobDTO());
        continue;
      }
      if (!verificationJob.getEnvIdentifierRuntimeParam().isRuntimeParam()) {
        EnvironmentResponseDTO environmentResponseDTO =
            nextGenService.getEnvironment(verificationJob.getEnvIdentifier(), accountId, orgIdentifier, projectId);

        if (environmentResponseDTO.getName().toLowerCase().contains(filter.trim().toLowerCase())) {
          verificationJobList.add(verificationJob.getVerificationJobDTO());
          continue;
        }
      }

      if (!verificationJob.getServiceIdentifierRuntimeParam().isRuntimeParam()) {
        ServiceResponseDTO serviceResponseDTO =
            nextGenService.getService(verificationJob.getServiceIdentifier(), accountId, orgIdentifier, projectId);

        if (serviceResponseDTO.getName().toLowerCase().contains(filter.trim().toLowerCase())) {
          verificationJobList.add(verificationJob.getVerificationJobDTO());
          continue;
        }
      }
    }

    return PageUtils.offsetAndLimit(verificationJobList, offset, pageSize);
  }

  private List<VerificationJob> verificationJobList(String accountId, String projectIdentifier, String orgIdentifier) {
    return hPersistence.createQuery(VerificationJob.class)
        .filter(VerificationJobKeys.accountId, accountId)
        .filter(VerificationJobKeys.orgIdentifier, orgIdentifier)
        .filter(VerificationJobKeys.projectIdentifier, projectIdentifier)
        .asList();
  }

  @Override
  public boolean doesAVerificationJobExistsForThisProject(
      String accountId, String orgIdentifier, String projectIdentifier) {
    long numberOfVerificationJobs = hPersistence.createQuery(VerificationJob.class)
                                        .filter(VerificationJobKeys.accountId, accountId)
                                        .filter(VerificationJobKeys.orgIdentifier, orgIdentifier)
                                        .filter(VerificationJobKeys.projectIdentifier, projectIdentifier)
                                        .count();
    return numberOfVerificationJobs > 0;
  }

  @Override
  public int getNumberOfServicesUndergoingHealthVerification(
      String accountId, String orgIdentifier, String projectIdentifier) {
    BasicDBObject verificationJobQuery = new BasicDBObject();
    List<BasicDBObject> conditions = new ArrayList<>();
    conditions.add(new BasicDBObject(VerificationJobKeys.accountId, accountId));
    conditions.add(new BasicDBObject(VerificationJobKeys.projectIdentifier, projectIdentifier));
    conditions.add(new BasicDBObject(VerificationJobKeys.orgIdentifier, orgIdentifier));
    conditions.add(new BasicDBObject(VerificationJobKeys.type, HEALTH.toString()));
    verificationJobQuery.put("$and", conditions);
    List<String> serviceIdentifiers = hPersistence.getCollection(VerificationJob.class)
                                          .distinct(VerificationJobKeys.serviceIdentifier, verificationJobQuery);
    return serviceIdentifiers.size();
  }

  @Override
  public void createDefaultHealthVerificationJob(String accountId, String orgIdentifier, String projectIdentifier) {
    VerificationJobDTO verificationJobDTO = HealthVerificationJobDTO.builder()
                                                .orgIdentifier(orgIdentifier)
                                                .projectIdentifier(projectIdentifier)
                                                .jobName(DEFAULT_HEALTH_JOB_NAME)
                                                .identifier(projectIdentifier + "_" + DEFAULT_HEALTH_JOB_ID)
                                                .dataSources(Arrays.asList(DataSourceType.values()))
                                                .monitoringSources(Arrays.asList("ALL"))
                                                .serviceIdentifier("${service}")
                                                .envIdentifier("${environment}")
                                                .duration("15m")
                                                .isDefaultJob(true)
                                                .build();
    this.upsert(accountId, verificationJobDTO);
  }

  @Override
  public VerificationJob getOrCreateDefaultHealthVerificationJob(
      String accountId, String orgIdentifier, String projectIdentifier) {
    VerificationJob defaultJob = getDefaultVerificationJob(accountId, orgIdentifier, projectIdentifier);
    if (defaultJob == null) {
      createDefaultHealthVerificationJob(accountId, orgIdentifier, projectIdentifier);
      defaultJob = getDefaultVerificationJob(accountId, orgIdentifier, projectIdentifier);
    }
    return defaultJob;
  }

  @Override
  public VerificationJobDTO getDefaultHealthVerificationJobDTO(
      String accountId, String orgIdentifier, String projectIdentifier) {
    VerificationJob defaultJob = getOrCreateDefaultHealthVerificationJob(accountId, orgIdentifier, projectIdentifier);
    return defaultJob.getVerificationJobDTO();
  }

  private VerificationJob getDefaultVerificationJob(String accountId, String orgIdentifier, String projectIdentifier) {
    return hPersistence.createQuery(VerificationJob.class)
        .filter(VerificationJobKeys.accountId, accountId)
        .filter(VerificationJobKeys.projectIdentifier, projectIdentifier)
        .filter(VerificationJobKeys.orgIdentifier, orgIdentifier)
        .filter(VerificationJobKeys.isDefaultJob, true)
        .get();
  }
}
