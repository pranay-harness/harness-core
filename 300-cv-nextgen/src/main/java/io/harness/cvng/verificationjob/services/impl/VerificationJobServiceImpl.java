package io.harness.cvng.verificationjob.services.impl;

import static io.harness.cvng.beans.job.VerificationJobType.HEALTH;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.activity.entities.CDNGActivitySource;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.services.api.UpdatableEntity;
import io.harness.cvng.verificationjob.entities.BlueGreenVerificationJob;
import io.harness.cvng.verificationjob.entities.CanaryVerificationJob;
import io.harness.cvng.verificationjob.entities.HealthVerificationJob;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobKeys;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobUpdatableEntity;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.persistence.HPersistence;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.mongodb.BasicDBObject;
import com.mongodb.DuplicateKeyException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@OwnedBy(HarnessTeam.CV)
public class VerificationJobServiceImpl implements VerificationJobService {
  @Inject private HPersistence hPersistence;
  @Inject private NextGenService nextGenService;
  @Inject private Injector injector;

  @Override
  @Nullable
  public VerificationJobDTO getVerificationJobDTO(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    VerificationJob verificationJob = getVerificationJob(accountId, orgIdentifier, projectIdentifier, identifier);
    if (verificationJob == null) {
      return null;
    }
    return verificationJob.getVerificationJobDTO();
  }

  @Override
  public void create(String accountId, VerificationJobDTO verificationJobDTO) {
    VerificationJob verificationJob = fromDto(verificationJobDTO);
    verificationJob.setAccountId(accountId);
    try {
      verificationJob.validate();
      hPersistence.save(verificationJob);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format(
              "A Verification Job  with identifier %s and orgIdentifier %s and projectIdentifier %s is already present",
              verificationJob.getIdentifier(), verificationJob.getOrgIdentifier(),
              verificationJob.getProjectIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public void update(String accountId, String identifier, VerificationJobDTO verificationJobDTO) {
    VerificationJob verificationJob = fromDto(verificationJobDTO);
    verificationJob.setAccountId(accountId);
    verificationJob.validate();
    VerificationJob stored = getVerificationJob(
        accountId, verificationJobDTO.getOrgIdentifier(), verificationJobDTO.getProjectIdentifier(), identifier);
    if (stored == null) {
      throw new InvalidRequestException(
          String.format(
              "Verification Job with identifier [%s] and orgIdentifier [%s] and projectIdentifier [%s] not found",
              identifier, verificationJobDTO.getOrgIdentifier(), verificationJobDTO.getProjectIdentifier()),
          USER);
    }

    UpdateOperations<VerificationJob> updateOperations = hPersistence.createUpdateOperations(VerificationJob.class);

    UpdatableEntity<VerificationJob, VerificationJobDTO> updatableEntity = injector.getInstance(
        Key.get(VerificationJobUpdatableEntity.class, Names.named(verificationJobDTO.getType().name())));
    updatableEntity.setUpdateOperations(updateOperations, verificationJobDTO);

    VerificationJob temp = getVerificationJob(
        accountId, verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier(), identifier);
    hPersistence.update(temp, updateOperations);
  }

  @Override
  public void save(VerificationJob verificationJob) {
    hPersistence.save(verificationJob);
  }

  @Override
  public VerificationJob getVerificationJob(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(orgIdentifier);
    Preconditions.checkNotNull(projectIdentifier);
    Preconditions.checkNotNull(identifier);
    return hPersistence.createQuery(VerificationJob.class)
        .filter(VerificationJobKeys.accountId, accountId)
        .filter(VerificationJobKeys.orgIdentifier, orgIdentifier)
        .filter(VerificationJobKeys.projectIdentifier, projectIdentifier)
        .filter(VerificationJobKeys.identifier, identifier)
        .get();
  }

  @Override
  public VerificationJob getResolvedHealthVerificationJob(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String envIdentifier, String serviceIdentifier) {
    Preconditions.checkNotNull(accountIdentifier);
    Preconditions.checkNotNull(orgIdentifier);
    Preconditions.checkNotNull(projectIdentifier);
    Preconditions.checkNotNull(envIdentifier);
    Preconditions.checkNotNull(serviceIdentifier);
    VerificationJob defaultHealthVerificationJob =
        getDefaultHealthVerificationJob(accountIdentifier, orgIdentifier, projectIdentifier);
    defaultHealthVerificationJob.setServiceIdentifier(serviceIdentifier, false);
    defaultHealthVerificationJob.setEnvIdentifier(envIdentifier, false);
    defaultHealthVerificationJob.setDuration("15m", false);
    return defaultHealthVerificationJob;
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
    return getVerificationJob(accountId, orgIdentifier, projectIdentifier, identifier);
  }

  @Override
  public VerificationJobDTO getDTOByUrl(String accountId, String verificationJobUrl) {
    VerificationJob job = getByUrl(accountId, verificationJobUrl);
    if (job != null) {
      VerificationJobDTO verificationJobDTO = job.getVerificationJobDTO();
      if (!job.getEnvIdentifierRuntimeParam().isRuntimeParam()) {
        EnvironmentResponseDTO environmentResponseDTO = nextGenService.getEnvironment(
            accountId, job.getOrgIdentifier(), job.getProjectIdentifier(), job.getEnvIdentifier());
        verificationJobDTO.setEnvName(environmentResponseDTO.getName());
      }
      if (!job.getServiceIdentifierRuntimeParam().isRuntimeParam()) {
        ServiceResponseDTO serviceResponseDTO = nextGenService.getService(
            accountId, job.getOrgIdentifier(), job.getProjectIdentifier(), job.getServiceIdentifier());
        verificationJobDTO.setServiceName(serviceResponseDTO.getName());
      }
      return verificationJobDTO;
    }
    return null;
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

  public void delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    hPersistence.delete(hPersistence.createQuery(VerificationJob.class)
                            .filter(VerificationJobKeys.accountId, accountId)
                            .filter(VerificationJobKeys.orgIdentifier, orgIdentifier)
                            .filter(VerificationJobKeys.projectIdentifier, projectIdentifier)
                            .filter(VerificationJobKeys.identifier, identifier));
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
            nextGenService.getEnvironment(accountId, orgIdentifier, projectId, verificationJob.getEnvIdentifier());

        if (environmentResponseDTO.getName().toLowerCase().contains(filter.trim().toLowerCase())) {
          verificationJobList.add(verificationJob.getVerificationJobDTO());
          continue;
        }
      }

      if (!verificationJob.getServiceIdentifierRuntimeParam().isRuntimeParam()) {
        ServiceResponseDTO serviceResponseDTO =
            nextGenService.getService(accountId, orgIdentifier, projectId, verificationJob.getServiceIdentifier());

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
  public VerificationJob getDefaultHealthVerificationJob(
      String accountId, String orgIdentifier, String projectIdentifier) {
    VerificationJob defaultJob = hPersistence.createQuery(VerificationJob.class)
                                     .filter(VerificationJobKeys.accountId, accountId)
                                     .filter(VerificationJobKeys.projectIdentifier, projectIdentifier)
                                     .filter(VerificationJobKeys.orgIdentifier, orgIdentifier)
                                     .filter(VerificationJobKeys.type, HEALTH)
                                     .filter(VerificationJobKeys.isDefaultJob, true)
                                     .get();
    Preconditions.checkNotNull(defaultJob,
        String.format(
            "Default Health job cannot be null for accountIdentifier [%s], orgIdentifier [%s], projectIdentifier [%s]",
            accountId, orgIdentifier, projectIdentifier));
    return defaultJob;
  }

  @Override
  public VerificationJobDTO getDefaultHealthVerificationJobDTO(
      String accountId, String orgIdentifier, String projectIdentifier) {
    VerificationJob defaultJob = getDefaultHealthVerificationJob(accountId, orgIdentifier, projectIdentifier);
    return defaultJob.getVerificationJobDTO();
  }

  @Override
  public VerificationJob fromDto(VerificationJobDTO verificationJobDTO) {
    Preconditions.checkNotNull(verificationJobDTO);
    VerificationJob job;
    switch (verificationJobDTO.getType()) {
      case HEALTH:
        job = new HealthVerificationJob();
        break;
      case CANARY:
        job = new CanaryVerificationJob();
        break;
      case TEST:
        job = new TestVerificationJob();
        break;
      case BLUE_GREEN:
        job = new BlueGreenVerificationJob();
        break;
      default:
        throw new IllegalStateException("Invalid type " + verificationJobDTO.getType());
    }
    job.fromDTO(verificationJobDTO);
    return job;
  }

  @Override
  public List<VerificationJobDTO> eligibleCDNGVerificationJobs(String accountId, String orgIdentifier,
      String projectIdentifier, String serviceIdentifier, String envIdentifier) {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(orgIdentifier);
    Preconditions.checkNotNull(projectIdentifier);
    Query<VerificationJob> query = hPersistence.createQuery(VerificationJob.class)
                                       .filter(VerificationJobKeys.accountId, accountId)
                                       .filter(VerificationJobKeys.orgIdentifier, orgIdentifier)
                                       .filter(VerificationJobKeys.projectIdentifier, projectIdentifier);
    query.or(query.criteria(VerificationJobKeys.activitySourceIdentifier)
                 .equal(CDNGActivitySource.CDNG_ACTIVITY_SOURCE_IDENTIFIER),
        query.criteria(VerificationJobKeys.activitySourceIdentifier).doesNotExist());
    if (serviceIdentifier != null) {
      query.or(query.criteria(VerificationJob.SERVICE_IDENTIFIER_IS_RUNTIME_PARAM_KEY).equal(true),
          query.criteria(VerificationJob.SERVICE_IDENTIFIER_VALUE_KEY).equal(serviceIdentifier));
    } else {
      query = query.filter(VerificationJob.SERVICE_IDENTIFIER_IS_RUNTIME_PARAM_KEY, true);
    }

    if (envIdentifier != null) {
      query.or(query.criteria(VerificationJob.ENV_IDENTIFIER_IS_RUNTIME_PARAM_KEY).equal(true),
          query.criteria(VerificationJob.ENV_IDENTIFIER_VALUE_KEY).equal(envIdentifier));
    } else {
      query = query.filter(VerificationJob.ENV_IDENTIFIER_IS_RUNTIME_PARAM_KEY, true);
    }

    return query.asList().stream().map(VerificationJob::getVerificationJobDTO).collect(Collectors.toList());
  }

  @Override
  public void createDefaultVerificationJobs(String accountId, String orgIdentifier, String projectIdentifier) {
    saveDefaultJob(HealthVerificationJob.createDefaultJob(accountId, orgIdentifier, projectIdentifier));
    saveDefaultJob(TestVerificationJob.createDefaultJob(accountId, orgIdentifier, projectIdentifier));
    saveDefaultJob(CanaryVerificationJob.createDefaultJob(accountId, orgIdentifier, projectIdentifier));
    saveDefaultJob(BlueGreenVerificationJob.createDefaultJob(accountId, orgIdentifier, projectIdentifier));
  }

  private void saveDefaultJob(VerificationJob verificationJob) {
    try {
      verificationJob.validate();
      hPersistence.save(verificationJob);
    } catch (DuplicateKeyException ex) {
      log.info(String.format(
          "A Default Verification Job  with identifier %s and orgIdentifier %s and projectIdentifier %s is already present",
          verificationJob.getIdentifier(), verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier()));
    }
  }
}
