package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.STARTS_WITH;
import static software.wings.beans.SortOrder.OrderType.ASC;
import static software.wings.beans.SortOrder.OrderType.DESC;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.GCS;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.exception.WingsException.USER;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.data.validator.EntityNameValidator;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.scheduler.ArtifactCollectionJob;
import software.wings.scheduler.PruneEntityJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.ownership.OwnedByArtifactStream;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.stencils.DataProvider;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.ArtifactType;
import software.wings.utils.Util;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.time.Duration;
import java.util.ArrayList;
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
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;
  @Inject private StencilPostProcessor stencilPostProcessor;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private BuildSourceService buildSourceService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private AppService appService;
  @Inject private TriggerService triggerService;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private YamlChangeSetHelper yamlChangeSetHelper;
  @Inject @Transient private transient FeatureFlagService featureFlagService;

  @Override
  public PageResponse<ArtifactStream> list(PageRequest<ArtifactStream> req) {
    return wingsPersistence.query(ArtifactStream.class, req);
  }

  @Override
  public ArtifactStream get(String appId, String artifactStreamId) {
    return wingsPersistence.get(ArtifactStream.class, appId, artifactStreamId);
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
    validateArtifactSourceData(artifactStream);

    return forceCreate(artifactStream);
  }

  @Override
  @ValidationGroups(Create.class)
  public ArtifactStream forceCreate(ArtifactStream artifactStream) {
    artifactStream.setSourceName(artifactStream.generateSourceName());
    setAutoPopulatedName(artifactStream);

    String id = wingsPersistence.save(artifactStream);
    ArtifactCollectionJob.addDefaultJob(jobScheduler, artifactStream.getAppId(), artifactStream.getUuid());

    executorService.submit(() -> { artifactStreamChangeSetAsync(artifactStream); });

    return get(artifactStream.getAppId(), id);
  }

  public void artifactStreamChangeSetAsync(ArtifactStream artifactStream) {
    String accountId = appService.getAccountIdByAppId(artifactStream.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();

      // add GitSyncFiles for trigger (artifact stream)
      changeSet.add(entityUpdateService.getArtifactStreamGitSyncFile(accountId, artifactStream, ChangeType.MODIFY));

      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
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
                                                    .addOrder("name", DESC)
                                                    .build();
      PageResponse<ArtifactStream> response = wingsPersistence.query(ArtifactStream.class, pageRequest);

      // If an entry exists with the given default name
      if (isNotEmpty(response)) {
        name = Util.getNameWithNextRevision(response.get(0).getName(), name);
      }
      artifactStream.setName(name);
    }
  }

  @Override
  @ValidationGroups(Update.class)
  public ArtifactStream update(ArtifactStream artifactStream) {
    ArtifactStream savedArtifactStream =
        wingsPersistence.get(ArtifactStream.class, artifactStream.getAppId(), artifactStream.getUuid());
    if (savedArtifactStream == null) {
      throw new NotFoundException("Artifact stream with id " + artifactStream.getUuid() + " not found");
    }
    validateArtifactSourceData(artifactStream);

    artifactStream.setSourceName(artifactStream.generateSourceName());
    artifactStream = wingsPersistence.saveAndGet(ArtifactStream.class, artifactStream);

    if (!savedArtifactStream.getSourceName().equals(artifactStream.getSourceName())) {
      executorService.submit(() -> triggerService.updateByApp(savedArtifactStream.getAppId()));
    }

    ArtifactStream finalArtifactStream = artifactStream;
    yamlChangeSetHelper.updateYamlChangeAsync(
        finalArtifactStream, savedArtifactStream, appService.getAccountIdByAppId(finalArtifactStream.getAppId()));

    return artifactStream;
  }

  private void validateArtifactSourceData(ArtifactStream artifactStream) {
    String artifactStreamType = artifactStream.getArtifactStreamType();
    if (DOCKER.name().equals(artifactStreamType) || ECR.name().equals(artifactStreamType)
        || GCR.name().equals(artifactStreamType) || ACR.name().equals(artifactStreamType)
        || ARTIFACTORY.name().equals(artifactStreamType)) {
      buildSourceService.validateArtifactSource(
          artifactStream.getAppId(), artifactStream.getSettingId(), artifactStream.getArtifactStreamAttributes());
    }
  }

  private void ensureArtifactStreamSafeToDelete(String appId, String artifactStreamId) {
    List<software.wings.beans.trigger.Trigger> triggers =
        triggerService.getTriggersHasArtifactStreamAction(appId, artifactStreamId);
    if (isEmpty(triggers)) {
      return;
    }
    List<String> triggerNames =
        triggers.stream().map(software.wings.beans.trigger.Trigger::getName).collect(Collectors.toList());
    throw new InvalidRequestException(
        format("Artifact Source associated as a trigger action to triggers [%s]", Joiner.on(", ").join(triggerNames)),
        USER);
  }

  @Override
  public boolean delete(String appId, String artifactStreamId) {
    return delete(appId, artifactStreamId, false);
  }

  private boolean delete(String appId, String artifactStreamId, boolean forceDelete) {
    ArtifactStream artifactStream = get(appId, artifactStreamId);
    if (artifactStream == null) {
      return true;
    }
    if (!forceDelete) {
      ensureArtifactStreamSafeToDelete(appId, artifactStreamId);
    }

    String accountId = appService.getAccountIdByAppId(artifactStream.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();

      changeSet.add(entityUpdateService.getArtifactStreamGitSyncFile(accountId, artifactStream, ChangeType.DELETE));
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }

    return pruneArtifactStream(appId, artifactStreamId);
  }

  private boolean pruneArtifactStream(String appId, String artifactStreamId) {
    PruneEntityJob.addDefaultJob(jobScheduler, ArtifactStream.class, appId, artifactStreamId, Duration.ofSeconds(5));

    return wingsPersistence.delete(ArtifactStream.class, appId, artifactStreamId);
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String artifactStreamId) {
    List<OwnedByArtifactStream> services =
        ServiceClassLocator.descendingServices(this, ArtifactStreamServiceImpl.class, OwnedByArtifactStream.class);
    PruneEntityJob.pruneDescendingEntities(
        services, descending -> descending.pruneByArtifactStream(appId, artifactStreamId));
  }

  @Override
  public List<ArtifactStream> getArtifactStreamsForService(String appId, String serviceId) {
    PageRequest pageRequest = aPageRequest()
                                  .addFilter("appId", EQ, appId)
                                  .addFilter("serviceId", EQ, serviceId)
                                  .addOrder("createdAt", ASC)
                                  .build();
    PageResponse pageResponse = wingsPersistence.query(ArtifactStream.class, pageRequest);
    return pageResponse.getResponse();
  }

  @Override
  public Map<String, String> getSupportedBuildSourceTypes(String appId, String serviceId) {
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
      return builder.build();
    } else if (service.getArtifactType().equals(ArtifactType.AWS_LAMBDA)) {
      return ImmutableMap.of(AMAZON_S3.name(), AMAZON_S3.name());
    } else if (service.getArtifactType().equals(ArtifactType.AMI)) {
      return ImmutableMap.of(AMI.name(), AMI.name());
    } else if (service.getArtifactType().equals(ArtifactType.OTHER)) {
      return new ImmutableMap.Builder<String, String>()
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
          .build();
    }

    return new ImmutableMap.Builder<String, String>()
        .put(ArtifactStreamType.JENKINS.name(), ArtifactStreamType.JENKINS.name())
        .put(ArtifactStreamType.BAMBOO.name(), ArtifactStreamType.BAMBOO.name())
        .put(GCS.name(), GCS.name())
        .put(ArtifactStreamType.NEXUS.name(), ArtifactStreamType.NEXUS.name())
        .put(ArtifactStreamType.ARTIFACTORY.name(), ArtifactStreamType.ARTIFACTORY.name())
        .put(AMAZON_S3.name(), AMAZON_S3.name())
        .build();
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
}
