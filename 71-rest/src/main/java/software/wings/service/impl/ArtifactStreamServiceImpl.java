package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.STARTS_WITH;
import static io.harness.beans.SortOrder.OrderType.ASC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.GCS;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.beans.artifact.ArtifactStreamType.SFTP;
import static software.wings.beans.artifact.ArtifactStreamType.SMB;
import static software.wings.common.Constants.REFERENCED_ENTITIES_TO_SHOW;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.data.validator.EntityNameValidator;
import io.harness.exception.InvalidRequestException;
import io.harness.queue.Queue;
import io.harness.scheduler.PersistentScheduler;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Event.Type;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.ArtifactSourceable;
import software.wings.dl.WingsPersistence;
import software.wings.prune.PruneEntityListener;
import software.wings.prune.PruneEvent;
import software.wings.scheduler.ArtifactCollectionJob;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.ownership.OwnedByArtifactStream;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.settings.SettingValue;
import software.wings.stencils.DataProvider;
import software.wings.utils.ArtifactType;
import software.wings.utils.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.NotFoundException;

@Singleton
@ValidateOnExecution
public class ArtifactStreamServiceImpl implements ArtifactStreamService, DataProvider {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactStreamService.class);

  private static final String SETTING_ID_KEY = "settingId";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;
  @Inject private Queue<PruneEvent> pruneQueue;
  @Inject @Named("ServiceJobScheduler") private PersistentScheduler serviceJobScheduler;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private BuildSourceService buildSourceService;
  @Inject private AppService appService;
  @Inject private TriggerService triggerService;
  @Inject private SettingsService settingsService;
  @Inject private ArtifactService artifactService; // Do not delete it is being used by Prune
  @Inject private YamlPushService yamlPushService;
  @Inject @Transient private transient FeatureFlagService featureFlagService;

  @Override
  public PageResponse<ArtifactStream> list(PageRequest<ArtifactStream> req) {
    return wingsPersistence.query(ArtifactStream.class, req);
  }

  @Override
  public ArtifactStream get(String appId, String artifactStreamId) {
    return wingsPersistence.getWithAppId(ArtifactStream.class, appId, artifactStreamId);
  }

  @Override
  public ArtifactStream getArtifactStreamByName(String appId, String serviceId, String artifactStreamName) {
    return wingsPersistence.createQuery(ArtifactStream.class)
        .filter("appId", appId)
        .filter("serviceId", serviceId)
        .filter("name", artifactStreamName)
        .get();
  }

  @Override
  @ValidationGroups(Create.class)
  public ArtifactStream create(ArtifactStream artifactStream) {
    return create(artifactStream, true);
  }

  @Override
  @ValidationGroups(Create.class)
  public ArtifactStream create(ArtifactStream artifactStream, boolean validate) {
    if (validate) {
      validateArtifactSourceData(artifactStream);
    }

    artifactStream.setSourceName(artifactStream.generateSourceName());
    setAutoPopulatedName(artifactStream);

    String id = wingsPersistence.save(artifactStream);
    String accountId = appService.getAccountIdByAppId(artifactStream.getAppId());

    ArtifactCollectionJob.addDefaultJob(
        serviceJobScheduler, accountId, artifactStream.getAppId(), artifactStream.getUuid());

    yamlPushService.pushYamlChangeSet(
        accountId, null, artifactStream, Type.CREATE, artifactStream.isSyncFromGit(), false);

    return get(artifactStream.getAppId(), id);
  }

  /**
   * This method gets the default name, checks if another entry exists with the same name, if exists, it parses and
   * extracts the revision and creates a name with the next revision.
   *
   * @param artifactStream
   */
  private void setAutoPopulatedName(ArtifactStream artifactStream) {
    if (artifactStream.isAutoPopulate()) {
      String name = EntityNameValidator.getMappedString(artifactStream.generateName());
      String escapedString = Pattern.quote(name);

      // We need to check if the name exists in case of auto generate, if it exists, we need to add a suffix to the
      // name.
      PageRequest<ArtifactStream> pageRequest = aPageRequest()
                                                    .addFilter("appId", EQ, artifactStream.getAppId())
                                                    .addFilter("serviceId", EQ, artifactStream.getServiceId())
                                                    .addFilter("name", STARTS_WITH, escapedString)
                                                    .build();
      PageResponse<ArtifactStream> response = wingsPersistence.query(ArtifactStream.class, pageRequest);

      // If an entry exists with the given default name
      if (isNotEmpty(response)) {
        name = Util.getNameWithNextRevision(
            response.getResponse().stream().map(ArtifactStream::getName).collect(toList()), name);
      }
      artifactStream.setName(name);
    }
  }

  @Override
  @ValidationGroups(Update.class)
  public ArtifactStream update(ArtifactStream artifactStream) {
    return update(artifactStream, true);
  }

  public ArtifactStream update(ArtifactStream artifactStream, boolean validate) {
    ArtifactStream existingArtifactStream =
        wingsPersistence.getWithAppId(ArtifactStream.class, artifactStream.getAppId(), artifactStream.getUuid());
    if (existingArtifactStream == null) {
      throw new NotFoundException("Artifact stream with id " + artifactStream.getUuid() + " not found");
    }

    if (validate) {
      validateArtifactSourceData(artifactStream);
    }

    artifactStream.setSourceName(artifactStream.generateSourceName());
    ArtifactStream finalArtifactStream = wingsPersistence.saveAndGet(ArtifactStream.class, artifactStream);

    if (!existingArtifactStream.getSourceName().equals(finalArtifactStream.getSourceName())) {
      executorService.submit(() -> triggerService.updateByApp(existingArtifactStream.getAppId()));
    }

    if (shouldDeleteArtifactsOnSourceChanged(existingArtifactStream, finalArtifactStream)) {
      // TODO: This logic has to be moved to Prune event or Queue to ensure guaranteed execution
      executorService.submit(() -> artifactService.deleteWhenArtifactSourceNameChanged(existingArtifactStream));
    }
    boolean isRename = !artifactStream.getName().equals(existingArtifactStream.getName());
    String accountId = appService.getAccountIdByAppId(artifactStream.getAppId());
    yamlPushService.pushYamlChangeSet(
        accountId, existingArtifactStream, finalArtifactStream, Type.UPDATE, artifactStream.isSyncFromGit(), isRename);
    return finalArtifactStream;
  }

  private boolean shouldDeleteArtifactsOnSourceChanged(
      ArtifactStream oldArtifactStream, ArtifactStream updatedArtifactStream) {
    ArtifactStreamType artifactStreamType = ArtifactStreamType.valueOf(oldArtifactStream.getArtifactStreamType());
    switch (artifactStreamType) {
      case CUSTOM:
        return false;
      case AMI:
      case ARTIFACTORY:
      case AMAZON_S3:
      case NEXUS:
      case ECR:
      case DOCKER:
      case GCR:
      case ACR:
      case GCS:
      case SMB:
      case SFTP:
      case JENKINS:
      case BAMBOO:
        return !oldArtifactStream.getSourceName().equals(updatedArtifactStream.getSourceName());

      default:
        throw new InvalidRequestException(
            "Artifact source changed check not covered for Artifact Stream Type [" + artifactStreamType + "]");
    }
  }

  private void validateArtifactSourceData(ArtifactStream artifactStream) {
    String artifactStreamType = artifactStream.getArtifactStreamType();
    if (DOCKER.name().equals(artifactStreamType) || ECR.name().equals(artifactStreamType)
        || GCR.name().equals(artifactStreamType) || ACR.name().equals(artifactStreamType)
        || ARTIFACTORY.name().equals(artifactStreamType)) {
      buildSourceService.validateArtifactSource(
          artifactStream.getAppId(), artifactStream.getSettingId(), artifactStream.fetchArtifactStreamAttributes());
    } else if (CUSTOM.name().equals(artifactStreamType)) {
      buildSourceService.validateArtifactSource(artifactStream);
    }
  }

  private void ensureArtifactStreamSafeToDelete(String appId, String artifactStreamId) {
    List<software.wings.beans.trigger.Trigger> triggers =
        triggerService.getTriggersHasArtifactStreamAction(appId, artifactStreamId);
    if (isEmpty(triggers)) {
      return;
    }
    List<String> triggerNames = triggers.stream().map(software.wings.beans.trigger.Trigger::getName).collect(toList());
    throw new InvalidRequestException(
        format("Artifact Source associated as a trigger action to triggers [%s]", Joiner.on(", ").join(triggerNames)),
        USER);
  }

  @Override
  public boolean delete(String appId, String artifactStreamId) {
    return delete(appId, artifactStreamId, false, false);
  }

  @Override
  public boolean deleteByYamlGit(String appId, String artifactStreamId, boolean syncFromGit) {
    return delete(appId, artifactStreamId, false, syncFromGit);
  }

  private boolean delete(String appId, String artifactStreamId, boolean forceDelete, boolean syncFromGit) {
    ArtifactStream artifactStream = get(appId, artifactStreamId);
    if (artifactStream == null) {
      return true;
    }
    if (!forceDelete) {
      ensureArtifactStreamSafeToDelete(appId, artifactStreamId);
    }

    String accountId = appService.getAccountIdByAppId(artifactStream.getAppId());
    yamlPushService.pushYamlChangeSet(accountId, artifactStream, null, Type.DELETE, syncFromGit, false);

    return pruneArtifactStream(appId, artifactStreamId);
  }

  private boolean pruneArtifactStream(String appId, String artifactStreamId) {
    pruneQueue.send(new PruneEvent(ArtifactStream.class, appId, artifactStreamId));
    return wingsPersistence.delete(ArtifactStream.class, appId, artifactStreamId);
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String artifactStreamId) {
    List<OwnedByArtifactStream> services =
        ServiceClassLocator.descendingServices(this, ArtifactStreamServiceImpl.class, OwnedByArtifactStream.class);
    PruneEntityListener.pruneDescendingEntities(
        services, descending -> descending.pruneByArtifactStream(appId, artifactStreamId));
  }

  @Override
  public boolean artifactStreamsExistForService(String appId, String serviceId) {
    return wingsPersistence.createQuery(ArtifactStream.class)
               .filter(ArtifactStream.APP_ID_KEY, appId)
               .filter(ArtifactStream.SERVICE_ID_KEY, serviceId)
               .getKey()
        != null;
  }

  @Override
  public List<ArtifactStream> getArtifactStreamsForService(String appId, String serviceId) {
    PageRequest pageRequest = aPageRequest()
                                  .addFilter("appId", EQ, appId)
                                  .addFilter("serviceId", EQ, serviceId)
                                  .addOrder(ArtifactStream.CREATED_AT_KEY, ASC)
                                  .build();
    PageResponse pageResponse = wingsPersistence.query(ArtifactStream.class, pageRequest);
    return pageResponse.getResponse();
  }

  @Override
  public Map<String, String> fetchArtifactSourceProperties(String accountId, String appId, String artifactStreamId) {
    ArtifactStream artifactStream = wingsPersistence.getWithAppId(ArtifactStream.class, appId, artifactStreamId);
    Map<String, String> artifactSourceProperties = new HashMap<>();
    if (artifactStream == null) {
      logger.warn("Failed to construct artifact source properties. Artifact Stream {} was deleted", artifactStreamId);
      return artifactSourceProperties;
    }
    SettingValue settingValue = settingsService.getSettingValueById(accountId, artifactStream.getSettingId());
    if (settingValue instanceof ArtifactSourceable) {
      artifactSourceProperties.putAll(((ArtifactSourceable) settingValue).fetchArtifactSourceProperties());
    }
    artifactSourceProperties.putAll(artifactStream.fetchArtifactSourceProperties());

    return artifactSourceProperties;
  }

  @Override
  public List<ArtifactStream> fetchArtifactStreamsForService(String appId, String serviceId) {
    return wingsPersistence.createQuery(ArtifactStream.class)
        .filter(ArtifactStream.APP_ID_KEY, appId)
        .filter(ArtifactStream.SERVICE_ID_KEY, serviceId)
        .asList();
  }

  @Override
  public List<String> fetchArtifactStreamIdsForService(String appId, String serviceId) {
    return wingsPersistence.createQuery(ArtifactStream.class)
        .filter(ArtifactStream.APP_ID_KEY, appId)
        .filter(ArtifactStream.SERVICE_ID_KEY, serviceId)
        .asKeyList()
        .stream()
        .map(artifactStreamKey -> artifactStreamKey.getId().toString())
        .collect(toList());
  }

  @Override
  public Map<String, String> getSupportedBuildSourceTypes(String appId, String serviceId) {
    String accountId = appService.getAccountIdByAppId(appId);
    Service service = serviceResourceService.get(appId, serviceId, false);
    // Observed NPE in logs due to invalid service id provided by the ui due to a stale screen.
    if (service == null) {
      throw new InvalidRequestException("Service does not exist", USER);
    }
    if (service.getArtifactType().equals(ArtifactType.DOCKER)) {
      ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>()
                                                         .put(DOCKER.name(), DOCKER.name())
                                                         .put(ECR.name(), ECR.name())
                                                         .put(ACR.name(), ACR.name())
                                                         .put(GCR.name(), GCR.name())
                                                         .put(ARTIFACTORY.name(), ARTIFACTORY.name())
                                                         .put(NEXUS.name(), NEXUS.name());

      if (featureFlagService.isEnabled(FeatureName.CUSTOM_ARTIFACT_SOURCE, accountId)) {
        builder.put(CUSTOM.name(), CUSTOM.name());
      }
      return builder.build();
    } else if (service.getArtifactType().equals(ArtifactType.AWS_LAMBDA)) {
      ImmutableMap.Builder<String, String> builder =
          new ImmutableMap.Builder<String, String>().put(AMAZON_S3.name(), AMAZON_S3.name());
      if (featureFlagService.isEnabled(FeatureName.CUSTOM_ARTIFACT_SOURCE, accountId)) {
        builder.put(CUSTOM.name(), CUSTOM.name());
      }
      return builder.build();
    } else if (service.getArtifactType().equals(ArtifactType.AMI)) {
      ImmutableMap.Builder<String, String> builder =
          new ImmutableMap.Builder<String, String>().put(AMI.name(), AMI.name());
      if (featureFlagService.isEnabled(FeatureName.CUSTOM_ARTIFACT_SOURCE, accountId)) {
        builder.put(CUSTOM.name(), CUSTOM.name());
      }
      return builder.build();
    } else if (service.getArtifactType().equals(ArtifactType.OTHER)) {
      ImmutableMap.Builder<String, String> builder =
          new ImmutableMap.Builder<String, String>()
              .put(DOCKER.name(), DOCKER.name())
              .put(ECR.name(), ECR.name())
              .put(ACR.name(), ACR.name())
              .put(GCR.name(), GCR.name())
              .put(ARTIFACTORY.name(), ARTIFACTORY.name())
              .put(NEXUS.name(), NEXUS.name())
              .put(ArtifactStreamType.JENKINS.name(), ArtifactStreamType.JENKINS.name())
              .put(ArtifactStreamType.BAMBOO.name(), ArtifactStreamType.BAMBOO.name())
              .put(GCS.name(), GCS.name())
              .put(AMAZON_S3.name(), AMAZON_S3.name())
              .put(SMB.name(), SMB.name())
              .put(AMI.name(), AMI.name())
              .put(SFTP.name(), SFTP.name());
      if (featureFlagService.isEnabled(FeatureName.CUSTOM_ARTIFACT_SOURCE, accountId)) {
        builder.put(CUSTOM.name(), CUSTOM.name());
      }
      return builder.build();
    }

    ImmutableMap.Builder<String, String> builder =
        new ImmutableMap.Builder<String, String>()
            .put(ArtifactStreamType.JENKINS.name(), ArtifactStreamType.JENKINS.name())
            .put(ArtifactStreamType.BAMBOO.name(), ArtifactStreamType.BAMBOO.name())
            .put(GCS.name(), GCS.name())
            .put(ArtifactStreamType.NEXUS.name(), ArtifactStreamType.NEXUS.name())
            .put(ArtifactStreamType.ARTIFACTORY.name(), ArtifactStreamType.ARTIFACTORY.name())
            .put(AMAZON_S3.name(), AMAZON_S3.name())
            .put(SMB.name(), SMB.name())
            .put(AMI.name(), AMI.name())
            .put(SFTP.name(), SFTP.name());

    if (featureFlagService.isEnabled(FeatureName.CUSTOM_ARTIFACT_SOURCE, accountId)) {
      builder.put(CUSTOM.name(), CUSTOM.name());
    }
    return builder.build();
  }

  @Override
  public void pruneByService(String appId, String serviceId) {
    wingsPersistence.createQuery(ArtifactStream.class)
        .filter(ArtifactStream.APP_ID_KEY, appId)
        .filter("serviceId", serviceId)
        .asList()
        .forEach(artifactSource -> pruneArtifactStream((String) appId, (String) artifactSource.getUuid()));
  }

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    return (Map<String, String>) list(aPageRequest().addFilter("appId", EQ, appId).build())
        .getResponse()
        .stream()
        .collect(Collectors.toMap(ArtifactStream::getUuid, ArtifactStream::getSourceName));
  }

  @Override
  public boolean updateFailedCronAttempts(String appId, String artifactStreamId, int counter) {
    Query<ArtifactStream> query = wingsPersistence.createQuery(ArtifactStream.class)
                                      .filter(ArtifactStream.APP_ID_KEY, appId)
                                      .filter(Mapper.ID_KEY, artifactStreamId);
    UpdateOperations<ArtifactStream> updateOperations =
        wingsPersistence.createUpdateOperations(ArtifactStream.class).set("failedCronAttempts", counter);
    UpdateResults update = wingsPersistence.update(query, updateOperations);
    return update.getUpdatedCount() == 1;
  }
  public List<ArtifactStream> listBySettingId(String settingId) {
    return wingsPersistence.createQuery(ArtifactStream.class, excludeAuthority)
        .filter(SETTING_ID_KEY, settingId)
        .asList(new FindOptions().limit(REFERENCED_ENTITIES_TO_SHOW));
  }
}
