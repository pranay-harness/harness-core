package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.ArtifactCollectionJob;
import software.wings.scheduler.PruneEntityJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.ownership.OwnedByArtifactStream;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.ArtifactType;
import software.wings.utils.Util;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.NotFoundException;

/**
 * The Class ArtifactStreamServiceImpl.
 */
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

  @Override
  public PageResponse<ArtifactStream> list(PageRequest<ArtifactStream> req) {
    PageResponse<ArtifactStream> pageResponse = wingsPersistence.query(ArtifactStream.class, req);
    return pageResponse;
  }

  @Override
  public ArtifactStream get(String appId, String artifactStreamId) {
    return wingsPersistence.get(ArtifactStream.class, appId, artifactStreamId);
  }

  @Override
  public ArtifactStream getArtifactStreamByName(String appId, String serviceId, String artifactStreamName) {
    return wingsPersistence.createQuery(ArtifactStream.class)
        .field("appId")
        .equal(appId)
        .field("serviceId")
        .equal(serviceId)
        .field("name")
        .equal(artifactStreamName)
        .get();
  }

  @Override
  @ValidationGroups(Create.class)
  public ArtifactStream create(ArtifactStream artifactStream) {
    if (DOCKER.name().equals(artifactStream.getArtifactStreamType())
        || ECR.name().equals(artifactStream.getArtifactStreamType())
        || GCR.name().equals(artifactStream.getArtifactStreamType())
        || ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType())) {
      buildSourceService.validateArtifactSource(
          artifactStream.getAppId(), artifactStream.getSettingId(), artifactStream.getArtifactStreamAttributes());
    }

    artifactStream.setSourceName(artifactStream.generateSourceName());
    if (artifactStream.isAutoPopulate()) {
      setAutoPopulatedName(artifactStream);
    }

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
    String name = artifactStream.generateName();

    String escapedString = Pattern.quote(name);

    // We need to check if the name exists in case of auto generate, if it exists, we need to add a suffix to the name.
    PageRequest<ArtifactStream> pageRequest = PageRequest.Builder.aPageRequest()
                                                  .addFilter("appId", Operator.EQ, artifactStream.getAppId())
                                                  .addFilter("serviceId", Operator.EQ, artifactStream.getServiceId())
                                                  .addFilter("name", Operator.STARTS_WITH, escapedString)
                                                  .addOrder("name", OrderType.DESC)
                                                  .build();
    PageResponse<ArtifactStream> response = wingsPersistence.query(ArtifactStream.class, pageRequest);

    // If an entry exists with the given default name
    if (response != null && response.size() > 0) {
      String existingName = response.get(0).getName();
      name = Util.getNameWithNextRevision(existingName, name);
    }

    artifactStream.setName(name);
  }

  @Override
  @ValidationGroups(Update.class)
  public ArtifactStream update(ArtifactStream artifactStream) {
    ArtifactStream savedArtifactStream =
        wingsPersistence.get(ArtifactStream.class, artifactStream.getAppId(), artifactStream.getUuid());
    if (savedArtifactStream == null) {
      throw new NotFoundException("Artifact stream with id " + artifactStream.getUuid() + " not found");
    }
    if (DOCKER.name().equals(artifactStream.getArtifactStreamType())
        || ECR.name().equals(artifactStream.getArtifactStreamType())
        || GCR.name().equals(artifactStream.getArtifactStreamType())
        || ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType())) {
      buildSourceService.validateArtifactSource(
          artifactStream.getAppId(), artifactStream.getSettingId(), artifactStream.getArtifactStreamAttributes());
    }

    artifactStream.setSourceName(artifactStream.generateSourceName());
    if (artifactStream.isAutoPopulate()) {
      setAutoPopulatedName(artifactStream);
    }

    artifactStream = wingsPersistence.saveAndGet(ArtifactStream.class, artifactStream);

    if (savedArtifactStream.getSourceName().equals(artifactStream.getSourceName())) {
      executorService.submit(() -> triggerService.updateByApp(savedArtifactStream.getAppId()));
    }

    ArtifactStream finalArtifactStream = artifactStream;
    executorService.submit(() -> artifactStreamChangeSetAsync(finalArtifactStream));

    return artifactStream;
  }

  private void ensureArtifactStreamSafeToDelete(String appId, String artifactStreamId) {
    List<software.wings.beans.trigger.Trigger> triggers =
        triggerService.getTriggersHasArtifactStreamAction(appId, artifactStreamId);
    if (CollectionUtils.isEmpty(triggers)) {
      return;
    }
    List<String> triggerNames =
        triggers.stream().map(software.wings.beans.trigger.Trigger::getName).collect(Collectors.toList());
    throw new WingsException(INVALID_REQUEST)
        .addParam("message",
            String.format(
                "Artifact Source associated as a trigger action to triggers [%s]", Joiner.on(", ").join(triggerNames)));
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

    PruneEntityJob.addDefaultJob(jobScheduler, ArtifactStream.class, appId, artifactStreamId);

    return wingsPersistence.delete(wingsPersistence.createQuery(ArtifactStream.class)
                                       .field(ID_KEY)
                                       .equal(artifactStreamId)
                                       .field(ArtifactStream.APP_ID_KEY)
                                       .equal(appId));
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String triggerId) {
    List<OwnedByArtifactStream> services =
        ServiceClassLocator.descendingServices(this, ArtifactStreamServiceImpl.class, OwnedByArtifactStream.class);
    PruneEntityJob.pruneDescendingEntities(
        services, appId, appId, descending -> descending.pruneByArtifactStream(appId, triggerId));
  }

  @Override
  public List<ArtifactStream> getArtifactStreamsForService(String appId, String serviceId) {
    PageRequest pageRequest = Builder.aPageRequest()
                                  .addFilter("appId", Operator.EQ, appId)
                                  .addFilter("serviceId", Operator.EQ, serviceId)
                                  .addOrder("createdAt", OrderType.ASC)
                                  .build();
    PageResponse pageResponse = wingsPersistence.query(ArtifactStream.class, pageRequest);
    return pageResponse.getResponse();
  }

  @Override
  public List<Stencil> getArtifactStreamSchema(String appId, String serviceId) {
    return stencilPostProcessor.postProcess(asList(ArtifactStreamType.values()), appId, serviceId);
  }

  @Override
  public Map<String, String> getSupportedBuildSourceTypes(String appId, String serviceId) {
    Service service = serviceResourceService.get(appId, serviceId);
    // Observed NPE in logs due to invalid service id provided by the ui due to a stale screen.
    if (service == null) {
      throw new WingsException("Service " + serviceId + "for the given app " + appId + "does not exist ");
    }
    if (service.getArtifactType().equals(ArtifactType.DOCKER)) {
      return ImmutableMap.of(DOCKER.name(), DOCKER.name(), ECR.name(), ECR.name(), GCR.name(), GCR.name(),
          ARTIFACTORY.name(), ARTIFACTORY.name(), NEXUS.name(), NEXUS.name());
    } else if (service.getArtifactType().equals(ArtifactType.AWS_LAMBDA)) {
      return ImmutableMap.of(AMAZON_S3.name(), AMAZON_S3.name());
    } else if (service.getArtifactType().equals(ArtifactType.AMI)) {
      return ImmutableMap.of(AMI.name(), AMI.name());
    }
    return ImmutableMap.of(ArtifactStreamType.JENKINS.name(), ArtifactStreamType.JENKINS.name(),
        ArtifactStreamType.BAMBOO.name(), ArtifactStreamType.BAMBOO.name(), ArtifactStreamType.NEXUS.name(),
        ArtifactStreamType.NEXUS.name(), ArtifactStreamType.ARTIFACTORY.name(), ArtifactStreamType.ARTIFACTORY.name(),
        AMAZON_S3.name(), AMAZON_S3.name());
  }

  @Override
  public void pruneByService(String appId, String serviceId) {
    wingsPersistence.createQuery(ArtifactStream.class)
        .field(ArtifactStream.APP_ID_KEY)
        .equal(appId)
        .field("serviceId")
        .equal(serviceId)
        .asList()
        .forEach(artifactSource -> delete((String) appId, (String) artifactSource.getUuid()));
  }

  @Override
  public Map<String, String> getData(String appId, String... params) {
    return (Map<String, String>) list(aPageRequest().addFilter("appId", EQ, appId).build())
        .getResponse()
        .stream()
        .collect(Collectors.toMap(ArtifactStream::getUuid, ArtifactStream::getSourceName));
  }
}
