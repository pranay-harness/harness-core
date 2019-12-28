package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.STARTS_WITH;
import static io.harness.data.structure.CollectionUtils.trimmedLowercaseSet;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.AZURE_ARTIFACTS;
import static software.wings.beans.artifact.ArtifactStreamType.BAMBOO;
import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.GCS;
import static software.wings.beans.artifact.ArtifactStreamType.JENKINS;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.beans.artifact.ArtifactStreamType.SFTP;
import static software.wings.beans.artifact.ArtifactStreamType.SMB;
import static software.wings.common.TemplateConstants.LATEST_TAG;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.validator.EntityNameValidator;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedUsageRestrictionsException;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.HIterator;
import io.harness.queue.QueuePublisher;
import io.harness.validation.Create;
import io.harness.validation.PersistenceValidator;
import io.harness.validation.Update;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.AccountEvent;
import software.wings.beans.AccountEventType;
import software.wings.beans.AzureContainerRegistry;
import software.wings.beans.EntityType;
import software.wings.beans.Event.Type;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.AcrArtifactStream;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactKeys;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.beans.artifact.ArtifactStreamCollectionStatus;
import software.wings.beans.artifact.ArtifactStreamSummary;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.ArtifactSummary;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.AzureArtifactsArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.config.ArtifactSourceable;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.trigger.Trigger;
import software.wings.dl.WingsPersistence;
import software.wings.prune.PruneEntityListener;
import software.wings.prune.PruneEvent;
import software.wings.service.impl.artifact.ArtifactComparator;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.AzureResourceService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.ownership.OwnedByArtifactStream;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.settings.SettingValue;
import software.wings.stencils.DataProvider;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryFormat;
import software.wings.utils.RepositoryType;
import software.wings.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.NotFoundException;

@Singleton
@ValidateOnExecution
@Slf4j
public class ArtifactStreamServiceImpl implements ArtifactStreamService, DataProvider {
  private static final Integer REFERENCED_ENTITIES_TO_SHOW = 10;

  // Restrict to docker only artifact streams.
  private static final List<String> dockerOnlyArtifactStreams = Collections.unmodifiableList(
      asList(ArtifactStreamType.DOCKER.name(), ECR.name(), GCR.name(), ACR.name(), CUSTOM.name()));

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;
  @Inject private QueuePublisher<PruneEvent> pruneQueue;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private BuildSourceService buildSourceService;
  @Inject private AppService appService;
  @Inject private SettingsService settingsService;
  @Inject private YamlPushService yamlPushService;
  @Inject @Transient private transient FeatureFlagService featureFlagService;
  @Inject private TemplateService templateService;
  @Inject private TemplateHelper templateHelper;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private transient AzureResourceService azureResourceService;

  // Do not delete as they are being used by Prune
  @Inject private ArtifactService artifactService;
  @Inject private AlertService alertService;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject private TriggerService triggerService;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private DeploymentTriggerService deploymentTriggerService;

  @Override
  public PageResponse<ArtifactStream> list(PageRequest<ArtifactStream> req) {
    return wingsPersistence.query(ArtifactStream.class, req);
  }

  @Override
  public PageResponse<ArtifactStream> list(
      PageRequest<ArtifactStream> req, String accountId, boolean withArtifactCount, String artifactSearchString) {
    return list(req, accountId, withArtifactCount, artifactSearchString, null, Integer.MAX_VALUE);
  }

  @Override
  public PageResponse<ArtifactStream> list(PageRequest<ArtifactStream> req, String accountId, boolean withArtifactCount,
      String artifactSearchString, ArtifactType artifactType, int maxArtifacts) {
    if (!withArtifactCount) {
      return list(req);
    }

    try {
      PageRequest<ArtifactStream> pageRequest = req.copy();
      int offset = pageRequest.getStart();
      int limit = pageRequest.getPageSize();

      pageRequest.setOffset("0");
      pageRequest.setLimit(String.valueOf(Integer.MAX_VALUE));
      addFilterToArtifactStreamPageRequest(artifactType, pageRequest);
      PageResponse<ArtifactStream> pageResponse = wingsPersistence.query(ArtifactStream.class, pageRequest);

      List<ArtifactStream> filteredArtifactStreams = pageResponse.getResponse();

      if (isNotEmpty(filteredArtifactStreams)) {
        filteredArtifactStreams = filterArtifactStreamsAndArtifactsWithCount(
            accountId, artifactSearchString, maxArtifacts, filteredArtifactStreams);
      }

      List<ArtifactStream> resp;
      if (isEmpty(filteredArtifactStreams)) {
        resp = Collections.emptyList();
      } else {
        int total = filteredArtifactStreams.size();
        if (total <= offset) {
          resp = Collections.emptyList();
        } else {
          int endIdx = Math.min(offset + limit, total);
          resp = filteredArtifactStreams.subList(offset, endIdx);
          resp.forEach(artifactStream -> {
            SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
            if (settingAttribute != null) {
              artifactStream.setArtifactServerName(settingAttribute.getName());
            }
          });
        }
      }

      return aPageResponse()
          .withResponse(resp)
          .withTotal(filteredArtifactStreams.size())
          .withOffset(req.getOffset())
          .withLimit(req.getLimit())
          .build();
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  private List<ArtifactStream> filterArtifactStreamsAndArtifactsWithCount(
      String accountId, String artifactSearchString, int maxArtifacts, List<ArtifactStream> filteredArtifactStreams) {
    List<ArtifactStream> newArtifactStreams = new ArrayList<>();
    for (ArtifactStream artifactStream : filteredArtifactStreams) {
      Query<Artifact> artifactQuery = wingsPersistence.createQuery(Artifact.class)
                                          .disableValidation()
                                          .filter(ArtifactKeys.artifactStreamId, artifactStream.getUuid())
                                          .filter(ArtifactKeys.accountId, accountId)
                                          .project(ArtifactKeys.artifactStreamId, true)
                                          .project(ArtifactKeys.uiDisplayName, true)
                                          .project(ArtifactKeys.metadata, true)
                                          .order(Sort.descending(CreatedAtAware.CREATED_AT_KEY));
      if (isNotEmpty(artifactSearchString)) {
        // NOTE: Might be inefficient for artifact streams having large number of artifacts.
        artifactQuery.or(artifactQuery.criteria(ArtifactKeys.uiDisplayName).containsIgnoreCase(artifactSearchString),
            artifactQuery.criteria(ArtifactKeys.metadata_buildNo).containsIgnoreCase(artifactSearchString));
      }

      long totalArtifactCount = artifactQuery.count();
      if (totalArtifactCount == 0L) {
        continue;
      }

      List<Artifact> artifacts = artifactQuery.asList(new FindOptions().limit(maxArtifacts));
      if (isEmpty(artifacts)) {
        continue;
      }

      List<ArtifactSummary> artifactSummaries = artifacts.stream()
                                                    .sorted(new ArtifactComparator())
                                                    .map(ArtifactSummary::prepareSummaryFromArtifact)
                                                    .collect(toList());
      artifactStream.setArtifactCount(totalArtifactCount);
      artifactStream.setArtifacts(artifactSummaries);
      newArtifactStreams.add(artifactStream);
    }
    return newArtifactStreams;
  }

  static void addFilterToArtifactStreamQuery(ArtifactType artifactType, Query<ArtifactStream> artifactStreamQuery) {
    if (artifactType == null) {
      return;
    }

    switch (artifactType) {
      case DOCKER: // Deployment type: K8S, ECS, Helm
        artifactStreamQuery.or(
            artifactStreamQuery.criteria(ArtifactStreamKeys.artifactStreamType).in(dockerOnlyArtifactStreams),
            artifactStreamQuery.criteria(ArtifactStreamKeys.repositoryFormat).equal(RepositoryFormat.docker.name()),
            artifactStreamQuery.criteria(ArtifactStreamKeys.repositoryType).equal(RepositoryType.docker.name()));
        break;
      case AWS_LAMBDA:
        artifactStreamQuery.criteria(ArtifactStreamKeys.artifactStreamType)
            .in(asList(AMAZON_S3.name(), CUSTOM.name())); // TODO: verify this
        break;
      case AMI:
        artifactStreamQuery.criteria(ArtifactStreamKeys.artifactStreamType).equal(AMI.name());
        break;
      case AWS_CODEDEPLOY: // Deployment Type: AWS_CODEDEPLOY,
      case PCF: // Deployment Type: PCF,
      case WAR: // Deployment type: ssh
      case JAR:
      case TAR:
      case RPM:
      case ZIP:
      case IIS: // Deployment type: WinRM
      case IIS_APP:
      case IIS_VirtualDirectory:
        artifactStreamQuery.and(
            artifactStreamQuery.criteria(ArtifactStreamKeys.artifactStreamType)
                .in(asList(JENKINS.name(), BAMBOO.name(), GCS.name(), NEXUS.name(), ARTIFACTORY.name(),
                    AMAZON_S3.name(), SMB.name(), AMI.name(), SFTP.name(), AZURE_ARTIFACTS.name(), CUSTOM.name())),
            artifactStreamQuery.criteria(ArtifactStreamKeys.repositoryFormat).notEqual(RepositoryFormat.docker.name()),
            artifactStreamQuery.criteria(ArtifactStreamKeys.repositoryType)
                .notEqual(RepositoryType.docker.name())); // TODO: verify this
        break;
      case OTHER:
        artifactStreamQuery.criteria(ArtifactStreamKeys.artifactStreamType)
            .in(asList(ArtifactStreamType.DOCKER.name(), ECR.name(), GCR.name(), ACR.name(), JENKINS.name(),
                BAMBOO.name(), GCS.name(), NEXUS.name(), ARTIFACTORY.name(), AMAZON_S3.name(), SMB.name(), AMI.name(),
                SFTP.name(), AZURE_ARTIFACTS.name(), CUSTOM.name()));
        break;
      default:
        throw new InvalidRequestException(format("Unsupported artifact type: [%s]", artifactType.name()));
    }
  }

  private static void addFilterToArtifactStreamPageRequest(
      ArtifactType artifactType, PageRequest<ArtifactStream> artifactStreamPageRequest) {
    if (artifactType == null) {
      return;
    }

    switch (artifactType) {
      case DOCKER: // Deployment type: K8S, ECS, Helm
        artifactStreamPageRequest.addFilter("", Operator.OR,
            createSearchFilter(ArtifactStreamKeys.artifactStreamType, Operator.IN, dockerOnlyArtifactStreams.toArray()),
            createSearchFilter(ArtifactStreamKeys.repositoryFormat, Operator.EQ, RepositoryFormat.docker.name()),
            createSearchFilter(ArtifactStreamKeys.repositoryType, Operator.EQ, RepositoryType.docker.name()));
        break;
      case AWS_LAMBDA:
        artifactStreamPageRequest.addFilter(
            ArtifactStreamKeys.artifactStreamType, Operator.IN, AMAZON_S3.name(), CUSTOM.name()); // TODO: verify this
        break;
      case AMI:
        artifactStreamPageRequest.addFilter(ArtifactStreamKeys.artifactStreamType, Operator.EQ, AMI.name());
        break;
      case AWS_CODEDEPLOY: // Deployment Type: AWS_CODEDEPLOY,
      case PCF: // Deployment Type: PCF,
      case WAR: // Deployment type: ssh
      case JAR:
      case TAR:
      case RPM:
      case ZIP:
      case IIS: // Deployment type: WinRM
      case IIS_APP:
      case IIS_VirtualDirectory:
        artifactStreamPageRequest.addFilter("", Operator.AND,
            createSearchFilter(ArtifactStreamKeys.artifactStreamType, Operator.IN, JENKINS.name(), BAMBOO.name(),
                GCS.name(), NEXUS.name(), ARTIFACTORY.name(), AMAZON_S3.name(), SMB.name(), AMI.name(), SFTP.name(),
                AZURE_ARTIFACTS.name(), CUSTOM.name()),
            createSearchFilter(ArtifactStreamKeys.repositoryFormat, Operator.NOT_EQ, RepositoryFormat.docker.name()),
            createSearchFilter(
                ArtifactStreamKeys.repositoryType, Operator.NOT_EQ, RepositoryType.docker.name())); // TODO: verify this
        break;
      case OTHER:
        artifactStreamPageRequest.addFilter(ArtifactStreamKeys.artifactStreamType, Operator.IN,
            ArtifactStreamType.DOCKER.name(), ECR.name(), GCR.name(), ACR.name(), JENKINS.name(), BAMBOO.name(),
            GCS.name(), NEXUS.name(), ARTIFACTORY.name(), AMAZON_S3.name(), SMB.name(), AMI.name(), SFTP.name(),
            AZURE_ARTIFACTS.name(), CUSTOM.name());
        break;
      default:
        throw new InvalidRequestException(format("Unsupported artifact type: [%s]", artifactType.name()));
    }
  }

  private static SearchFilter createSearchFilter(String fieldName, Operator op, Object... fieldValues) {
    return SearchFilter.builder().fieldName(fieldName).op(op).fieldValues(fieldValues).build();
  }

  @Override
  public ArtifactStream get(String artifactStreamId) {
    return wingsPersistence.get(ArtifactStream.class, artifactStreamId);
  }

  @Override
  public ArtifactStream getArtifactStreamByName(String appId, String serviceId, String artifactStreamName) {
    return artifactStreamServiceBindingService.listArtifactStreams(appId, serviceId)
        .stream()
        .filter(artifactStream -> artifactStreamName.equals(artifactStream.getName()))
        .findFirst()
        .orElse(null);
  }

  @Override
  public ArtifactStream getArtifactStreamByName(String settingId, String artifactStreamName) {
    return wingsPersistence.createQuery(ArtifactStream.class)
        .filter(ArtifactStreamKeys.appId, GLOBAL_APP_ID)
        .filter(ArtifactStreamKeys.settingId, settingId)
        .filter(ArtifactStreamKeys.name, artifactStreamName)
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
    String accountId = getAccountIdForArtifactStream(artifactStream);
    artifactStream.setAccountId(accountId);

    if (GLOBAL_APP_ID.equals(artifactStream.fetchAppId())) {
      usageRestrictionsService.validateUsageRestrictionsOnEntitySave(artifactStream.getAccountId(),
          settingsService.getUsageRestrictionsForSettingId(artifactStream.getSettingId()));
    }

    setServiceId(artifactStream);
    artifactStream.validateRequiredFields();
    if (validate && artifactStream.getTemplateUuid() == null) {
      validateArtifactSourceData(artifactStream);
    }

    artifactStream.setSourceName(artifactStream.generateSourceName());
    setAutoPopulatedName(artifactStream);
    if (!artifactStream.isAutoPopulate() && isEmpty(artifactStream.getName())) {
      throw new InvalidRequestException("Artifact source name is mandatory", USER);
    }

    if (artifactStream.getTemplateUuid() != null) {
      String version = artifactStream.getTemplateVersion() != null ? artifactStream.getTemplateVersion() : LATEST_TAG;
      ArtifactStream artifactStream1 = (ArtifactStream) templateService.constructEntityFromTemplate(
          artifactStream.getTemplateUuid(), version, EntityType.ARTIFACT_STREAM);
      if (artifactStream instanceof CustomArtifactStream) {
        ((CustomArtifactStream) artifactStream).setScripts(((CustomArtifactStream) artifactStream1).getScripts());
      }
      if (isEmpty(artifactStream.getTemplateVariables())) {
        artifactStream.setTemplateVariables(artifactStream1.getTemplateVariables());
      } else {
        if (validate) {
          validateArtifactSourceData(artifactStream);
        }
      }
    }

    addAcrHostNameIfNeeded(artifactStream);

    // Set metadata-only field for nexus and azure artifacts.
    setMetadataOnly(artifactStream);
    // Add keywords.
    artifactStream.setKeywords(trimmedLowercaseSet(artifactStream.generateKeywords()));
    // Set collection status initially to UNSTABLE.
    artifactStream.setCollectionStatus(ArtifactStreamCollectionStatus.UNSTABLE.name());
    String id = PersistenceValidator.duplicateCheck(
        () -> wingsPersistence.save(artifactStream), "name", artifactStream.getName());
    yamlPushService.pushYamlChangeSet(
        accountId, null, artifactStream, Type.CREATE, artifactStream.isSyncFromGit(), false);

    if (!artifactStream.isSample()) {
      eventPublishHelper.publishAccountEvent(accountId,
          AccountEvent.builder().accountEventType(AccountEventType.ARTIFACT_STREAM_ADDED).build(), true, true);
    }

    return get(id);
  }

  private void setServiceId(ArtifactStream artifactStream) {
    // set serviceId equal to settingId for connector level artifact streams
    String appId = artifactStream.fetchAppId();
    if (appId == null || appId.equals(GLOBAL_APP_ID)) {
      artifactStream.setServiceId(artifactStream.getSettingId());
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

      // TODO: ASR: IMP: update this
      // We need to check if the name exists in case of auto generate, if it exists, we need to add a suffix to the
      // name.
      PageRequest<ArtifactStream> pageRequest = aPageRequest()
                                                    .addFilter("appId", EQ, artifactStream.fetchAppId())
                                                    .addFilter("serviceId", EQ, artifactStream.getServiceId())
                                                    .addFilter("name", STARTS_WITH, name)
                                                    .build();
      PageResponse<ArtifactStream> response = wingsPersistence.query(ArtifactStream.class, pageRequest);

      // For the connector level also, check if the name exists
      // TODO: ASR: uncomment when index added on setting_id + name
      //      pageRequest = aPageRequest()
      //                        .addFilter("settingId", EQ, artifactStream.getSettingId())
      //                        .addFilter("name", STARTS_WITH, escapedString)
      //                        .build();
      //      PageResponse<ArtifactStream> connectorResponse = wingsPersistence.query(ArtifactStream.class,
      //      pageRequest);
      // If an entry exists with the given default name
      //      if (isNotEmpty(response) || isNotEmpty(connectorResponse)) {
      if (isNotEmpty(response)) {
        name = Utils.getNameWithNextRevision(
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
    return update(artifactStream, validate, false);
  }

  public ArtifactStream update(ArtifactStream artifactStream, boolean validate, boolean fromTemplate) {
    ArtifactStream existingArtifactStream = wingsPersistence.get(ArtifactStream.class, artifactStream.getUuid());
    if (existingArtifactStream == null) {
      throw new NotFoundException("Artifact stream with id " + artifactStream.getUuid() + " not found");
    }

    String accountId = getAccountIdForArtifactStream(artifactStream);
    artifactStream.setAccountId(accountId);

    if (GLOBAL_APP_ID.equals(artifactStream.fetchAppId())) {
      usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(accountId,
          settingsService.getUsageRestrictionsForSettingId(existingArtifactStream.getSettingId()),
          settingsService.getUsageRestrictionsForSettingId(artifactStream.getSettingId()));
    }

    artifactStream.validateRequiredFields();

    if (artifactStream.getArtifactStreamType() != null && existingArtifactStream.getArtifactStreamType() != null
        && !artifactStream.getArtifactStreamType().equals(existingArtifactStream.getArtifactStreamType())) {
      throw new InvalidRequestException("Artifact Stream type cannot be updated", USER);
    }

    if (artifactStream.getAccountId() != null && existingArtifactStream.getAccountId() != null
        && !artifactStream.getAccountId().equals(existingArtifactStream.getAccountId())) {
      throw new InvalidRequestException("Artifact Stream cannot be moved from one account to another", USER);
    } else {
      artifactStream.setAccountId(existingArtifactStream.getAccountId());
    }

    setMetadataOnly(artifactStream);
    if (artifactStream.isMetadataOnly() != existingArtifactStream.isMetadataOnly()) {
      throw new InvalidRequestException("Artifact Stream's metadata-only property cannot be changed", USER);
    }

    artifactStream.setSourceName(artifactStream.generateSourceName());

    // For artifactory.
    validateRepositoryType(artifactStream, existingArtifactStream);
    // For nexus.
    validateRepositoryFormat(artifactStream, existingArtifactStream);
    validateExtensionAndClassifier(artifactStream, existingArtifactStream);
    // For azure artifacts.
    validateProtocolType(artifactStream, existingArtifactStream);

    // for CUSTOM, update scripts and variables only if its not coming from template.
    // If coming from template, the update has already been handled in
    // software.wings.service.impl.template.ArtifactSourceTemplateProcessor.updateLinkedEntities()
    if (!fromTemplate) {
      populateCustomArtifactStreamFields(artifactStream, existingArtifactStream);
    }

    if (validate) {
      validateArtifactSourceData(artifactStream);
    }

    addAcrHostNameIfNeeded(artifactStream);

    // Add keywords.
    artifactStream.setKeywords(trimmedLowercaseSet(artifactStream.generateKeywords()));

    ArtifactStream finalArtifactStream = PersistenceValidator.duplicateCheck(
        () -> wingsPersistence.saveAndGet(ArtifactStream.class, artifactStream), "name", artifactStream.getName());

    if (!existingArtifactStream.getSourceName().equals(finalArtifactStream.getSourceName())) {
      if (CUSTOM.name().equals(artifactStream.getArtifactStreamType())) {
        artifactService.updateArtifactSourceName(finalArtifactStream);
      }
      executorService.submit(() -> triggerService.updateByArtifactStream(artifactStream.getUuid()));
    }

    if (shouldDeleteArtifactsOnSourceChanged(existingArtifactStream, finalArtifactStream)) {
      // Mark the collection status as unstable (for non-custom) because the artifact source has changed. We will again
      // do a fresh artifact collection.
      if (!CUSTOM.name().equals(artifactStream.getArtifactStreamType())) {
        updateCollectionStatus(
            accountId, finalArtifactStream.getUuid(), ArtifactStreamCollectionStatus.UNSTABLE.name());
      }

      // TODO: This logic has to be moved to Prune event or Queue to ensure guaranteed execution
      executorService.submit(() -> artifactService.deleteWhenArtifactSourceNameChanged(existingArtifactStream));
    }

    if (isEmpty(artifactStream.getName())) {
      throw new InvalidRequestException("Please provide valid artifact name", USER);
    }

    boolean isRename = !artifactStream.getName().equals(existingArtifactStream.getName());
    yamlPushService.pushYamlChangeSet(artifactStream.getAccountId(), existingArtifactStream, finalArtifactStream,
        Type.UPDATE, artifactStream.isSyncFromGit(), isRename);

    return finalArtifactStream;
  }

  private void populateCustomArtifactStreamFields(
      ArtifactStream artifactStream, ArtifactStream existingArtifactStream) {
    boolean versionChanged = false;
    List<Variable> oldTemplateVariables = existingArtifactStream.getTemplateVariables();
    if (artifactStream.getTemplateVersion() != null && existingArtifactStream.getTemplateVersion() != null
        && !artifactStream.getTemplateVersion().equals(existingArtifactStream.getTemplateVersion())) {
      versionChanged = true;
    }

    if (versionChanged || existingArtifactStream.getTemplateUuid() == null) {
      if (artifactStream.getTemplateUuid() != null) {
        String version = artifactStream.getTemplateVersion() != null ? artifactStream.getTemplateVersion() : LATEST_TAG;
        ArtifactStream artifactStreamFromTemplate = (ArtifactStream) templateService.constructEntityFromTemplate(
            artifactStream.getTemplateUuid(), version, EntityType.ARTIFACT_STREAM);
        notNullCheck("Template does not exist", artifactStreamFromTemplate, USER);
        // merge template variables and existing artifact stream values
        List<Variable> templateVariables = templateHelper.overrideVariables(
            artifactStreamFromTemplate.getTemplateVariables(), oldTemplateVariables, false);
        // Merge incoming template variables from payload with the template variables computed above
        // incoming template variable values get precedence over the computed values
        if (isNotEmpty(artifactStream.getTemplateVariables()) && isNotEmpty(templateVariables)) {
          for (Variable templateVariable : templateVariables) {
            for (Variable artifactStreamVariable : artifactStream.getTemplateVariables()) {
              if (templateVariable.getName().equals(artifactStreamVariable.getName())) {
                templateVariable.setValue(artifactStreamVariable.getValue());
              }
            }
          }
        }
        artifactStream.setTemplateVariables(templateVariables);
        if (artifactStream.getArtifactStreamType().equals(CUSTOM.name())) {
          ((CustomArtifactStream) artifactStream)
              .setScripts(((CustomArtifactStream) artifactStreamFromTemplate).getScripts());
        }
      }
    } else if (existingArtifactStream.getTemplateUuid() != null) {
      // through yaml flow: when only template variables are changed we update those but preserve the existing script
      artifactStream.setTemplateVariables(
          templateHelper.overrideVariables(artifactStream.getTemplateVariables(), oldTemplateVariables, false));
      if (artifactStream.getArtifactStreamType().equals(CUSTOM.name())) {
        ((CustomArtifactStream) artifactStream)
            .setScripts(((CustomArtifactStream) existingArtifactStream).getScripts());
      }
    }
  }

  private String getAccountIdForArtifactStream(ArtifactStream artifactStream) {
    String accountId = null;
    String appId = artifactStream.fetchAppId();
    if (appId == null || !appId.equals(GLOBAL_APP_ID)) {
      accountId = appService.getAccountIdByAppId(appId);
    } else {
      if (artifactStream.getSettingId() != null) {
        accountId = settingsService.fetchAccountIdBySettingId(artifactStream.getSettingId());
      }
    }
    return accountId;
  }

  private void validateRepositoryType(ArtifactStream artifactStream, ArtifactStream existingArtifactStream) {
    if (artifactStream != null && existingArtifactStream != null) {
      if (artifactStream.getArtifactStreamType().equals(ARTIFACTORY.name())) {
        if (!((ArtifactoryArtifactStream) artifactStream)
                 .getRepositoryType()
                 .equals(((ArtifactoryArtifactStream) existingArtifactStream).getRepositoryType())) {
          throw new InvalidRequestException("Repository Type cannot be updated", USER);
        }
      }
    }
  }

  private void validateRepositoryFormat(ArtifactStream artifactStream, ArtifactStream existingArtifactStream) {
    if (artifactStream != null && existingArtifactStream != null) {
      if (artifactStream.getArtifactStreamType().equals(NEXUS.name())) {
        NexusArtifactStream nexusArtifactStream = (NexusArtifactStream) artifactStream;
        if (nexusArtifactStream.getRepositoryFormat() == null
            || !nexusArtifactStream.getRepositoryFormat().equals(
                   ((NexusArtifactStream) existingArtifactStream).getRepositoryFormat())) {
          throw new InvalidRequestException("Repository Format cannot be updated", USER);
        }
      }
    }
  }

  private void validateExtensionAndClassifier(ArtifactStream artifactStream, ArtifactStream existingArtifactStream) {
    if (artifactStream != null && existingArtifactStream != null) {
      if (artifactStream instanceof NexusArtifactStream
          && RepositoryFormat.maven.name().equals(((NexusArtifactStream) artifactStream).getRepositoryFormat())
          && !existingArtifactStream.artifactSourceChanged(artifactStream)) {
        String extension = ((NexusArtifactStream) artifactStream).getExtension();
        String existingExtension = ((NexusArtifactStream) existingArtifactStream).getExtension();
        if ((extension == null && existingExtension != null) || (extension != null && existingExtension == null)
            || (extension != null && !extension.equals(existingExtension))) {
          throw new InvalidRequestException("Extension cannot be updated", USER);
        }

        String classifier = ((NexusArtifactStream) artifactStream).getClassifier();
        String existingClassifier = ((NexusArtifactStream) existingArtifactStream).getClassifier();
        if ((classifier == null && existingClassifier != null) || (classifier != null && existingClassifier == null)
            || (classifier != null && !classifier.equals(existingClassifier))) {
          throw new InvalidRequestException("Classifier cannot be updated", USER);
        }
      }
    }
  }

  private void validateProtocolType(ArtifactStream artifactStream, ArtifactStream existingArtifactStream) {
    if (artifactStream != null && existingArtifactStream != null) {
      if (artifactStream.getArtifactStreamType().equals(AZURE_ARTIFACTS.name())) {
        if (!((AzureArtifactsArtifactStream) artifactStream)
                 .getProtocolType()
                 .equals(((AzureArtifactsArtifactStream) existingArtifactStream).getProtocolType())) {
          throw new InvalidRequestException("Protocol type cannot be updated", USER);
        }
      }
    }
  }

  // TODO: move this to individual ArtifactStream classes instead of handling here
  private void setMetadataOnly(ArtifactStream artifactStream) {
    if (artifactStream != null && artifactStream.getArtifactStreamType().equals(NEXUS.name())) {
      if (RepositoryFormat.docker.name().equals(((NexusArtifactStream) artifactStream).getRepositoryFormat())) {
        artifactStream.setMetadataOnly(true);
      }
    }
    if (artifactStream != null && AZURE_ARTIFACTS.name().equals(artifactStream.getArtifactStreamType())) {
      artifactStream.setMetadataOnly(true);
    }
  }

  private void addAcrHostNameIfNeeded(ArtifactStream artifactStream) {
    if (artifactStream instanceof AcrArtifactStream) {
      AcrArtifactStream acrArtifactStream = (AcrArtifactStream) artifactStream;
      AzureContainerRegistry registry =
          azureResourceService
              .listContainerRegistries(acrArtifactStream.getSettingId(), acrArtifactStream.getSubscriptionId())
              .stream()
              .filter(item -> item.getName().equals(acrArtifactStream.getRegistryName()))
              .findFirst()
              .get();
      acrArtifactStream.setRegistryHostName(registry.getLoginServer());
    }
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
      case AZURE_ARTIFACTS:
        return oldArtifactStream.artifactSourceChanged(updatedArtifactStream);

      default:
        throw new InvalidRequestException(
            "Artifact source changed check not covered for Artifact Stream Type [" + artifactStreamType + "]");
    }
  }

  private void validateArtifactSourceData(ArtifactStream artifactStream) {
    String artifactStreamType = artifactStream.getArtifactStreamType();
    if (DOCKER.name().equals(artifactStreamType) || ECR.name().equals(artifactStreamType)
        || GCR.name().equals(artifactStreamType) || ACR.name().equals(artifactStreamType)
        || ARTIFACTORY.name().equals(artifactStreamType) || NEXUS.name().equals(artifactStreamType)
        || AZURE_ARTIFACTS.name().equals(artifactStreamType)) {
      if (artifactStream.shouldValidate()) {
        buildSourceService.validateArtifactSource(
            artifactStream.fetchAppId(), artifactStream.getSettingId(), artifactStream.fetchArtifactStreamAttributes());
      }
    } else if (CUSTOM.name().equals(artifactStreamType) && artifactStream.shouldValidate()) {
      buildSourceService.validateArtifactSource(artifactStream);
    }
  }

  private void ensureArtifactStreamSafeToDelete(String appId, String artifactStreamId, String accountId) {
    if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
      validateTriggerUsages(accountId, appId, artifactStreamId);
    } else if (appId.equals(GLOBAL_APP_ID)) {
      validateTriggerUsages(accountId, appId, artifactStreamId);
      validateServiceVariableUsages(artifactStreamId);
      validateWorkflowUsages(artifactStreamId);
    }
  }

  private void validateServiceVariableUsages(String artifactStreamId) {
    List<Service> services = artifactStreamServiceBindingService.listServices(artifactStreamId);
    if (isEmpty(services)) {
      return;
    }

    List<String> serviceNames = services.stream().map(Service::getName).collect(toList());
    throw new InvalidRequestException(
        format("Artifact Stream linked to Services [%s]", Joiner.on(", ").join(serviceNames)), USER);
  }

  private void validateWorkflowUsages(String artifactStreamId) {
    List<Workflow> workflows = artifactStreamServiceBindingService.listWorkflows(artifactStreamId);
    if (isEmpty(workflows)) {
      return;
    }
    List<String> workflowNames = workflows.stream().map(Workflow::getName).collect(toList());
    throw new InvalidRequestException(
        format("Artifact Stream linked to Workflows [%s]", Joiner.on(", ").join(workflowNames)), USER);
  }

  private void validateTriggerUsages(String accountId, String appId, String artifactStreamId) {
    List<String> triggerNames;
    if (featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, accountId)) {
      triggerNames = deploymentTriggerService.getTriggersHasArtifactStreamAction(accountId, appId, artifactStreamId);
    } else {
      List<Trigger> triggers = triggerService.getTriggersHasArtifactStreamAction(appId, artifactStreamId);
      if (isEmpty(triggers)) {
        return;
      }
      triggerNames = triggers.stream().map(Trigger::getName).collect(toList());
    }

    if (isNotEmpty(triggerNames)) {
      throw new InvalidRequestException(
          format("Artifact Stream associated as a trigger action to triggers [%s]", Joiner.on(", ").join(triggerNames)),
          USER);
    }
  }

  @Override
  public boolean delete(String appId, String artifactStreamId) {
    return delete(appId, artifactStreamId, false, false);
  }

  @Override
  public boolean delete(String artifactStreamId, boolean syncFromGit) {
    ArtifactStream artifactStream = get(artifactStreamId);
    if (artifactStream == null) {
      return true;
    }

    if (!GLOBAL_APP_ID.equals(artifactStream.fetchAppId())) {
      // delete method used only for artifact streams defined at connector level
      throw new NotFoundException("Artifact stream with id " + artifactStreamId + " not found");
    }

    // TODO: check if used in triggers
    String accountId = settingsService.fetchAccountIdBySettingId(artifactStream.getSettingId());
    if (GLOBAL_APP_ID.equals(artifactStream.fetchAppId())) {
      if (!usageRestrictionsService.userHasPermissionsToChangeEntity(
              accountId, settingsService.getUsageRestrictionsForSettingId(artifactStream.getSettingId()))) {
        throw new UnauthorizedUsageRestrictionsException(USER);
      }
      ensureArtifactStreamSafeToDelete(GLOBAL_APP_ID, artifactStreamId, accountId);
    }

    yamlPushService.pushYamlChangeSet(accountId, artifactStream, null, Type.DELETE, syncFromGit, false);
    return pruneArtifactStream(artifactStream.fetchAppId(), artifactStreamId);
  }

  private boolean delete(String appId, String artifactStreamId, boolean forceDelete, boolean syncFromGit) {
    ArtifactStream artifactStream = get(artifactStreamId);
    if (artifactStream == null) {
      return true;
    }

    return delete(appId, artifactStream, forceDelete, syncFromGit);
  }

  private boolean delete(String appId, ArtifactStream artifactStream, boolean forceDelete, boolean syncFromGit) {
    String accountId = null;
    if (!GLOBAL_APP_ID.equals(appId)) {
      accountId = appService.getAccountIdByAppId(artifactStream.getAppId());
    } else {
      if (artifactStream.getSettingId() != null) {
        accountId = settingsService.fetchAccountIdBySettingId(artifactStream.getSettingId());
        if (!usageRestrictionsService.userHasPermissionsToChangeEntity(
                accountId, settingsService.getUsageRestrictionsForSettingId(artifactStream.getSettingId()))) {
          throw new UnauthorizedUsageRestrictionsException(USER);
        }
      }
    }

    String artifactStreamId = artifactStream.getUuid();
    if (!forceDelete) {
      ensureArtifactStreamSafeToDelete(appId, artifactStreamId, accountId);
    }

    yamlPushService.pushYamlChangeSet(accountId, artifactStream, null, Type.DELETE, syncFromGit, false);
    return pruneArtifactStream(appId, artifactStreamId);
  }

  @Override
  public boolean pruneArtifactStream(String appId, String artifactStreamId) {
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
    return isNotEmpty(artifactStreamServiceBindingService.listArtifactStreamIds(appId, serviceId));
  }

  @Override
  public List<ArtifactStream> getArtifactStreamsForService(String appId, String serviceId) {
    return artifactStreamServiceBindingService.listArtifactStreams(appId, serviceId);
  }

  @Override
  public Map<String, String> fetchArtifactSourceProperties(String accountId, String artifactStreamId) {
    ArtifactStream artifactStream = wingsPersistence.get(ArtifactStream.class, artifactStreamId);
    Map<String, String> artifactSourceProperties = new HashMap<>();
    if (artifactStream == null) {
      logger.warn("Failed to construct artifact source properties. ArtifactStream {} was deleted", artifactStreamId);
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
    return artifactStreamServiceBindingService.listArtifactStreams(appId, serviceId);
  }

  @Override
  public List<String> fetchArtifactStreamIdsForService(String appId, String serviceId) {
    return artifactStreamServiceBindingService.listArtifactStreamIds(appId, serviceId);
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
                                                         .put(NEXUS.name(), NEXUS.name())
                                                         .put(CUSTOM.name(), CUSTOM.name());
      return builder.build();
    } else if (service.getArtifactType().equals(ArtifactType.AWS_LAMBDA)) {
      ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>()
                                                         .put(AMAZON_S3.name(), AMAZON_S3.name())
                                                         .put(CUSTOM.name(), CUSTOM.name());
      return builder.build();
    } else if (service.getArtifactType().equals(ArtifactType.AMI)) {
      ImmutableMap.Builder<String, String> builder =
          new ImmutableMap.Builder<String, String>().put(AMI.name(), AMI.name()).put(CUSTOM.name(), CUSTOM.name());
      return builder.build();
    } else if (service.getArtifactType().equals(ArtifactType.OTHER)) {
      ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>()
                                                         .put(DOCKER.name(), DOCKER.name())
                                                         .put(ECR.name(), ECR.name())
                                                         .put(ACR.name(), ACR.name())
                                                         .put(GCR.name(), GCR.name())
                                                         .put(ARTIFACTORY.name(), ARTIFACTORY.name())
                                                         .put(NEXUS.name(), NEXUS.name())
                                                         .put(JENKINS.name(), JENKINS.name())
                                                         .put(BAMBOO.name(), BAMBOO.name())
                                                         .put(GCS.name(), GCS.name())
                                                         .put(AMAZON_S3.name(), AMAZON_S3.name())
                                                         .put(AMI.name(), AMI.name())
                                                         .put(AZURE_ARTIFACTS.name(), AZURE_ARTIFACTS.name())
                                                         .put(SMB.name(), SMB.name())
                                                         .put(SFTP.name(), SFTP.name())
                                                         .put(CUSTOM.name(), CUSTOM.name());
      return builder.build();
    }

    ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>()
                                                       .put(ARTIFACTORY.name(), ARTIFACTORY.name())
                                                       .put(NEXUS.name(), NEXUS.name())
                                                       .put(JENKINS.name(), JENKINS.name())
                                                       .put(BAMBOO.name(), BAMBOO.name())
                                                       .put(GCS.name(), GCS.name())
                                                       .put(AMAZON_S3.name(), AMAZON_S3.name())
                                                       .put(AMI.name(), AMI.name())
                                                       .put(AZURE_ARTIFACTS.name(), AZURE_ARTIFACTS.name())
                                                       .put(SMB.name(), SMB.name())
                                                       .put(SFTP.name(), SFTP.name())
                                                       .put(CUSTOM.name(), CUSTOM.name());
    return builder.build();
  }

  @Override
  public void pruneByService(String appId, String serviceId) {
    // TODO: ASR: IMP: remove this after refactor - currently doesn't allow deleting if appId is GLOBAL_APP_ID
    if (appId == null || GLOBAL_APP_ID.equals(appId)) {
      return;
    }

    wingsPersistence.createQuery(ArtifactStream.class)
        .filter(ArtifactStreamKeys.appId, appId)
        .filter(ArtifactStreamKeys.serviceId, serviceId)
        .asList()
        .forEach(artifactStream -> {
          pruneArtifactStream(appId, artifactStream.getUuid());
          auditServiceHelper.reportDeleteForAuditing(appId, artifactStream);
        });
  }

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    if (appId == null || GLOBAL_APP_ID.equals(appId)) {
      return new HashMap<>();
    }

    List<ArtifactStream> artifactStreams = listByAppId(appId);
    if (isEmpty(artifactStreams)) {
      return new HashMap<>();
    }

    Map<String, String> data = new HashMap<>();
    artifactStreams.forEach(artifactStream -> data.put(artifactStream.getUuid(), artifactStream.getSourceName()));
    return data;
  }

  @Override
  public boolean updateFailedCronAttempts(String accountId, String artifactStreamId, int counter) {
    Query<ArtifactStream> query = wingsPersistence.createQuery(ArtifactStream.class)
                                      .filter(ArtifactStreamKeys.accountId, accountId)
                                      .filter(ArtifactStreamKeys.uuid, artifactStreamId);
    UpdateOperations<ArtifactStream> updateOperations = wingsPersistence.createUpdateOperations(ArtifactStream.class)
                                                            .set(ArtifactStreamKeys.failedCronAttempts, counter);
    UpdateResults update = wingsPersistence.update(query, updateOperations);
    return update.getUpdatedCount() == 1;
  }

  @Override
  public boolean updateCollectionStatus(String accountId, String artifactStreamId, String collectionStatus) {
    Query<ArtifactStream> query = wingsPersistence.createQuery(ArtifactStream.class)
                                      .filter(ArtifactStreamKeys.accountId, accountId)
                                      .filter(ArtifactStreamKeys.uuid, artifactStreamId);
    UpdateOperations<ArtifactStream> updateOperations = wingsPersistence.createUpdateOperations(ArtifactStream.class);
    setUnset(updateOperations, ArtifactStreamKeys.collectionStatus, collectionStatus);
    UpdateResults update = wingsPersistence.update(query, updateOperations);
    return update.getUpdatedCount() == 1;
  }

  @Override
  public List<ArtifactStream> listBySettingId(String settingId) {
    return wingsPersistence.createQuery(ArtifactStream.class, excludeAuthority)
        .filter(ArtifactStreamKeys.settingId, settingId)
        .asList(new FindOptions().limit(REFERENCED_ENTITIES_TO_SHOW));
  }

  @Override
  public List<ArtifactStream> listByIds(Collection<String> artifactStreamIds) {
    if (isEmpty(artifactStreamIds)) {
      return new ArrayList<>();
    }

    List<ArtifactStream> artifactStreams = wingsPersistence.createQuery(ArtifactStream.class, excludeAuthority)
                                               .field("_id")
                                               .in(artifactStreamIds)
                                               .asList();

    if (isNotEmpty(artifactStreams)) {
      List<ArtifactStream> orderedArtifactStreams = new ArrayList<>();
      Map<String, ArtifactStream> artifactStreamMap =
          artifactStreams.stream().collect(Collectors.toMap(ArtifactStream::getUuid, Function.identity()));
      for (String artifactStreamId : artifactStreamIds) {
        if (artifactStreamMap.containsKey(artifactStreamId)) {
          orderedArtifactStreams.add(artifactStreamMap.get(artifactStreamId));
        }
      }
      return orderedArtifactStreams;
    }
    return new ArrayList<>();
  }

  @Override
  public List<ArtifactStreamSummary> listArtifactStreamSummary(String appId) {
    Map<String, ArtifactStream> artifactStreamMap = getArtifactStreamMap(appId);
    List<ArtifactStreamSummary> artifactStreamSummaries = new ArrayList<>();
    String accountId = appService.getAccountIdByAppId(appId);
    if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
      try (HIterator<Service> serviceHIterator = new HIterator<>(wingsPersistence.createQuery(Service.class)
                                                                     .filter(ServiceKeys.appId, appId)
                                                                     .project(ServiceKeys.uuid, true)
                                                                     .project(ServiceKeys.name, true)
                                                                     .project(ServiceKeys.artifactStreamIds, true)
                                                                     .fetch())) {
        for (Service service : serviceHIterator) {
          List<String> artifactStreamIds = service.getArtifactStreamIds();
          if (isEmpty(artifactStreamIds)) {
            continue;
          }

          String serviceName = service.getName();
          artifactStreamSummaries.addAll(artifactStreamIds.stream()
                                             .filter(artifactStreamMap::containsKey)
                                             .map(artifactStreamId -> {
                                               ArtifactStream artifactStream = artifactStreamMap.get(artifactStreamId);
                                               return ArtifactStreamSummary.builder()
                                                   .artifactStreamId(artifactStreamId)
                                                   .settingId(artifactStream.getSettingId())
                                                   .displayName(artifactStream.getName() + " (" + serviceName + ")")
                                                   .build();
                                             })
                                             .collect(Collectors.toList()));
        }
      }

      return artifactStreamSummaries;
    }

    try (HIterator<Service> serviceHIterator = new HIterator<>(wingsPersistence.createQuery(Service.class)
                                                                   .filter(ServiceKeys.appId, appId)
                                                                   .project(ServiceKeys.uuid, true)
                                                                   .project(ServiceKeys.name, true)
                                                                   .fetch())) {
      for (Service service : serviceHIterator) {
        List<String> artifactStreamIds = artifactStreamServiceBindingService.listArtifactStreamIds(service);
        if (isEmpty(artifactStreamIds)) {
          continue;
        }

        String serviceName = service.getName();
        artifactStreamSummaries.addAll(artifactStreamIds.stream()
                                           .filter(artifactStreamMap::containsKey)
                                           .map(artifactStreamId -> {
                                             ArtifactStream artifactStream = artifactStreamMap.get(artifactStreamId);
                                             return ArtifactStreamSummary.builder()
                                                 .artifactStreamId(artifactStreamId)
                                                 .settingId(artifactStream.getSettingId())
                                                 .displayName(artifactStream.getName() + " (" + serviceName + ")")
                                                 .build();
                                           })
                                           .collect(Collectors.toList()));
      }
    }

    return artifactStreamSummaries;
  }

  private Map<String, ArtifactStream> getArtifactStreamMap(String appId) {
    String accountId = appService.getAccountIdByAppId(appId);

    Map<String, ArtifactStream> artifactStreamMap = new HashMap<>();
    try (HIterator<ArtifactStream> artifactStreamHIterator =
             new HIterator<>(wingsPersistence.createQuery(ArtifactStream.class)
                                 .filter(ArtifactStreamKeys.accountId, accountId)
                                 .project(ArtifactStreamKeys.uuid, true)
                                 .project(ArtifactStreamKeys.name, true)
                                 .fetch())) {
      for (ArtifactStream artifactStream : artifactStreamHIterator) {
        artifactStreamMap.put(artifactStream.getUuid(), artifactStream);
      }
    }
    return artifactStreamMap;
  }

  @Override
  @ValidationGroups(Create.class)
  public ArtifactStream createWithBinding(String appId, ArtifactStream artifactStream, boolean validate) {
    if (GLOBAL_APP_ID.equals(artifactStream.fetchAppId())) {
      return create(artifactStream, validate);
    }

    String serviceId = artifactStream.getServiceId();
    notNullCheck("ArtifactStream.serviceId", serviceId);

    // TODO: ASR: IMP: hack to make yaml push work as yaml changes require binding info but the binding info is deleted
    // in parallel
    artifactStream.setService(serviceResourceService.getWithDetails(appId, serviceId));

    // NOTE: artifactStream and binding must be created atomically
    ArtifactStream savedArtifactStream = create(artifactStream, validate);
    try {
      artifactStreamServiceBindingService.createOld(appId, serviceId, savedArtifactStream.getUuid());
    } catch (Exception e) {
      delete(appId, savedArtifactStream, false, false);
      throw e;
    }

    if (!artifactStream.isSample()) {
      eventPublishHelper.publishAccountEvent(savedArtifactStream.getAccountId(),
          AccountEvent.builder().accountEventType(AccountEventType.ARTIFACT_STREAM_ADDED).build(), true, true);
    }

    return savedArtifactStream;
  }

  @Override
  @ValidationGroups(Create.class)
  public boolean deleteWithBinding(String appId, String artifactStreamId, boolean forceDelete, boolean syncFromGit) {
    if (GLOBAL_APP_ID.equals(appId)) {
      return delete(artifactStreamId, syncFromGit);
    }

    ArtifactStream artifactStream = get(artifactStreamId);
    if (artifactStream == null) {
      return false;
    }

    String serviceId = artifactStream.getServiceId();
    notNullCheck("ArtifactStream.serviceId", serviceId);

    // TODO: ASR: IMP: hack to make yaml push work as yaml changes require binding info but the binding info is deleted
    // in parallel
    artifactStream.setService(serviceResourceService.getWithDetails(appId, serviceId));

    // NOTE: artifactStream and binding must be deleted atomically
    boolean retVal = delete(appId, artifactStream, forceDelete, syncFromGit);
    try {
      artifactStreamServiceBindingService.deleteOld(appId, serviceId, artifactStreamId);
    } catch (Exception e) {
      create(artifactStream, false);
      throw e;
    }

    return retVal;
  }

  @Override
  public List<ArtifactStream> listBySettingId(String appId, String settingId) {
    return wingsPersistence.createQuery(ArtifactStream.class, excludeAuthority)
        .filter(ArtifactStreamKeys.appId, appId)
        .filter(ArtifactStreamKeys.settingId, settingId)
        .asList();
  }

  @Override
  public List<ArtifactStream> listByAppId(String appId) {
    if (GLOBAL_APP_ID.equals(appId)) {
      return new ArrayList<>();
    }

    Set<String> artifactStreamIds = new HashSet<>();
    serviceResourceService.findServicesByApp(appId).forEach(service -> {
      List<String> serviceArtifactStreamIds = artifactStreamServiceBindingService.listArtifactStreamIds(service);
      if (isNotEmpty(serviceArtifactStreamIds)) {
        artifactStreamIds.addAll(serviceArtifactStreamIds);
      }
    });

    return listByIds(artifactStreamIds);
  }
}
