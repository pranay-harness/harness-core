package io.harness.cvng.core.services.impl;

import static io.harness.NGConstants.ENTITY_REFERENCE_LOG_PREFIX;
import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;

import io.harness.beans.IdentifierRef;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO.KubernetesActivitySourceConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVEventService;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entitysetupusage.DeleteSetupUsageDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class CVEventServiceImpl implements CVEventService {
  @Inject @Named(ENTITY_CRUD) private AbstractProducer eventProducer;
  @Inject private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;

  @Override
  public void sendConnectorCreateEvent(CVConfig cvConfig) {
    IdentifierRefProtoDTO configReference = getIdentifierRefProtoDTOFromConfig(cvConfig);
    IdentifierRef identifierRef = getIdentifierRef(cvConfig, cvConfig.getConnectorIdentifier());
    IdentifierRefProtoDTO configReferenceConnector = getIdentifierRefProtoDTOFromIdentifierRef(identifierRef);
    EntitySetupUsageCreateDTO connectorEntityReferenceDTO = getEntitySetupUsageCreateDTO(
        cvConfig, configReference, configReferenceConnector, EntityTypeProtoEnum.CONNECTORS);

    sendEventWithMessageForCreation(cvConfig, connectorEntityReferenceDTO);
  }

  @Override
  public void sendConnectorDeleteEvent(CVConfig cvConfig) {
    IdentifierRef identifierRef = getIdentifierRef(cvConfig, cvConfig.getConnectorIdentifier());
    String cvConfigConnectorFQN = getFullyQualifiedIdentifierFromIdentifierRef(identifierRef);
    String cvConfigFQN = getCVConfigFullyQualifiedName(cvConfig, cvConfig.getIdentifier());
    DeleteSetupUsageDTO deleteSetupUsageDTO =
        getDeleteSetupUsageDTO(cvConfig, cvConfigConnectorFQN, EntityTypeProtoEnum.CONNECTORS, cvConfigFQN);

    sendEventWithMessageForDeletion(cvConfig, deleteSetupUsageDTO);
  }

  @Override
  public void sendServiceCreateEvent(CVConfig cvConfig) {
    IdentifierRefProtoDTO configReference = getIdentifierRefProtoDTOFromConfig(cvConfig);
    IdentifierRef identifierRef = getIdentifierRef(cvConfig, cvConfig.getServiceIdentifier());
    IdentifierRefProtoDTO configReferenceService = getIdentifierRefProtoDTOFromIdentifierRef(identifierRef);
    EntitySetupUsageCreateDTO serviceEntityReferenceDTO =
        getEntitySetupUsageCreateDTO(cvConfig, configReference, configReferenceService, EntityTypeProtoEnum.SERVICE);

    sendEventWithMessageForCreation(cvConfig, serviceEntityReferenceDTO);
  }

  @Override
  public void sendServiceDeleteEvent(CVConfig cvConfig) {
    IdentifierRef identifierRef = getIdentifierRef(cvConfig, cvConfig.getServiceIdentifier());
    String cvConfigServiceFQN = getFullyQualifiedIdentifierFromIdentifierRef(identifierRef);
    String cvConfigFQN = getCVConfigFullyQualifiedName(cvConfig, cvConfig.getIdentifier());
    DeleteSetupUsageDTO deleteSetupUsageDTO =
        getDeleteSetupUsageDTO(cvConfig, cvConfigServiceFQN, EntityTypeProtoEnum.SERVICE, cvConfigFQN);

    sendEventWithMessageForDeletion(cvConfig, deleteSetupUsageDTO);
  }

  @Override
  public void sendEnvironmentCreateEvent(CVConfig cvConfig) {
    IdentifierRefProtoDTO configReference = getIdentifierRefProtoDTOFromConfig(cvConfig);
    IdentifierRef identifierRef = getIdentifierRef(cvConfig, cvConfig.getEnvIdentifier());
    IdentifierRefProtoDTO configReferenceEnvironment = getIdentifierRefProtoDTOFromIdentifierRef(identifierRef);
    EntitySetupUsageCreateDTO environmentEntityReferenceDTO = getEntitySetupUsageCreateDTO(
        cvConfig, configReference, configReferenceEnvironment, EntityTypeProtoEnum.ENVIRONMENT);

    sendEventWithMessageForCreation(cvConfig, environmentEntityReferenceDTO);
  }

  @Override
  public void sendEnvironmentDeleteEvent(CVConfig cvConfig) {
    IdentifierRef identifierRef = getIdentifierRef(cvConfig, cvConfig.getEnvIdentifier());
    String cvConfigEnvironmentFQN = getFullyQualifiedIdentifierFromIdentifierRef(identifierRef);
    String cvConfigFQN = getCVConfigFullyQualifiedName(cvConfig, cvConfig.getIdentifier());
    DeleteSetupUsageDTO deleteSetupUsageDTO =
        getDeleteSetupUsageDTO(cvConfig, cvConfigEnvironmentFQN, EntityTypeProtoEnum.ENVIRONMENT, cvConfigFQN);

    sendEventWithMessageForDeletion(cvConfig, deleteSetupUsageDTO);
  }

  @Override
  public void sendVerificationJobEnvironmentCreateEvent(VerificationJob verificationJob) {
    IdentifierRefProtoDTO verificationJobReference = getIdentifierRefProtoDTOFromVerificationJob(verificationJob);
    IdentifierRef identifierRef = getIdentifierRefVerificationJob(verificationJob, verificationJob.getEnvIdentifier());
    IdentifierRefProtoDTO verificationJobReferenceEnvironment =
        getIdentifierRefProtoDTOFromIdentifierRef(identifierRef);
    EntitySetupUsageCreateDTO environmentEntityReferenceDTO = getEntitySetupUsageCreateDTO(verificationJob,
        verificationJobReference, verificationJobReferenceEnvironment, EntityTypeProtoEnum.ENVIRONMENT);

    sendEventWithMessageForCreation(verificationJob, environmentEntityReferenceDTO);
  }

  @Override
  public void sendVerificationJobServiceCreateEvent(VerificationJob verificationJob) {
    IdentifierRefProtoDTO verificationJobReference = getIdentifierRefProtoDTOFromVerificationJob(verificationJob);
    IdentifierRef identifierRef =
        getIdentifierRefVerificationJob(verificationJob, verificationJob.getServiceIdentifier());
    IdentifierRefProtoDTO verificationJobReferenceService = getIdentifierRefProtoDTOFromIdentifierRef(identifierRef);
    EntitySetupUsageCreateDTO serviceEntityReferenceDTO = getEntitySetupUsageCreateDTO(
        verificationJob, verificationJobReference, verificationJobReferenceService, EntityTypeProtoEnum.SERVICE);

    sendEventWithMessageForCreation(verificationJob, serviceEntityReferenceDTO);
  }

  @Override
  public void sendVerificationJobEnvironmentDeleteEvent(VerificationJob verificationJob) {
    IdentifierRef identifierRef = getIdentifierRefVerificationJob(verificationJob, verificationJob.getEnvIdentifier());
    String verificationJobEnvironmentFQN = getFullyQualifiedIdentifierFromIdentifierRef(identifierRef);
    String verificationJobFQN = getVerificationJobFullyQualifiedName(verificationJob, verificationJob.getIdentifier());
    DeleteSetupUsageDTO deleteSetupUsageDTO = getDeleteSetupUsageDTO(
        verificationJob, verificationJobEnvironmentFQN, EntityTypeProtoEnum.ENVIRONMENT, verificationJobFQN);

    sendEventWithMessageForDeletion(verificationJob, deleteSetupUsageDTO);
  }

  @Override
  public void sendKubernetesActivitySourceConnectorCreateEvent(KubernetesActivitySource kubernetesActivitySource) {
    IdentifierRefProtoDTO kubernetesActivitySourceReference =
        getIdentifierRefProtoDTOFromKubernetesActivitySource(kubernetesActivitySource);
    IdentifierRef identifierRef =
        getIdentifierRefKubernetesSource(kubernetesActivitySource, kubernetesActivitySource.getConnectorIdentifier());
    IdentifierRefProtoDTO kubernetesActivitySourceReferenceConnector =
        getIdentifierRefProtoDTOFromIdentifierRef(identifierRef);
    EntitySetupUsageCreateDTO connectorEntityReferenceDTO = getEntitySetupUsageCreateDTO(kubernetesActivitySource,
        kubernetesActivitySourceReference, kubernetesActivitySourceReferenceConnector, EntityTypeProtoEnum.CONNECTORS);

    sendEventWithMessageForCreation(kubernetesActivitySource, connectorEntityReferenceDTO);
  }

  @Override
  public void sendVerificationJobServiceDeleteEvent(VerificationJob verificationJob) {
    IdentifierRef identifierRef =
        getIdentifierRefVerificationJob(verificationJob, verificationJob.getServiceIdentifier());
    String verificationJobEnvironmentFQN = getFullyQualifiedIdentifierFromIdentifierRef(identifierRef);
    String verificationJobFQN = getVerificationJobFullyQualifiedName(verificationJob, verificationJob.getIdentifier());
    DeleteSetupUsageDTO deleteSetupUsageDTO = getDeleteSetupUsageDTO(
        verificationJob, verificationJobEnvironmentFQN, EntityTypeProtoEnum.SERVICE, verificationJobFQN);

    sendEventWithMessageForDeletion(verificationJob, deleteSetupUsageDTO);
  }

  @Override
  public void sendKubernetesActivitySourceConnectorDeleteEvent(KubernetesActivitySource kubernetesActivitySource) {
    IdentifierRef identifierRef =
        getIdentifierRefKubernetesSource(kubernetesActivitySource, kubernetesActivitySource.getConnectorIdentifier());
    String kubernetesActivitySourceConnectorFQN = getFullyQualifiedIdentifierFromIdentifierRef(identifierRef);
    String kubernetesActivitySourceFQN = getKubernetesActivitySourceFullyQualifiedName(
        kubernetesActivitySource, kubernetesActivitySource.getIdentifier());
    DeleteSetupUsageDTO deleteSetupUsageDTO = getDeleteSetupUsageDTO(kubernetesActivitySource,
        EntityTypeProtoEnum.CONNECTORS, kubernetesActivitySourceConnectorFQN, kubernetesActivitySourceFQN);

    sendEventWithMessageForDeletion(kubernetesActivitySource, deleteSetupUsageDTO);
  }

  @Override
  public void sendKubernetesActivitySourceServiceCreateEvent(KubernetesActivitySource kubernetesActivitySource) {
    Set<KubernetesActivitySourceConfig> activitySourceConfigs = kubernetesActivitySource.getActivitySourceConfigs();

    String serviceIdentifier = null;
    if (hasSome(activitySourceConfigs)) {
      for (KubernetesActivitySourceConfig config : activitySourceConfigs) {
        serviceIdentifier = config.getServiceIdentifier();
      }
    }
    IdentifierRefProtoDTO kubernetesActivitySourceReference =
        getIdentifierRefProtoDTOFromKubernetesActivitySource(kubernetesActivitySource);
    IdentifierRef identifierRef = getIdentifierRefKubernetesSource(kubernetesActivitySource, serviceIdentifier);
    IdentifierRefProtoDTO kubernetesActivitySourceReferenceService =
        getIdentifierRefProtoDTOFromIdentifierRef(identifierRef);
    EntitySetupUsageCreateDTO serviceEntityReferenceDTO = getEntitySetupUsageCreateDTO(kubernetesActivitySource,
        kubernetesActivitySourceReference, kubernetesActivitySourceReferenceService, EntityTypeProtoEnum.SERVICE);

    sendEventWithMessageForCreation(kubernetesActivitySource, serviceEntityReferenceDTO);
  }

  @Override
  public void sendKubernetesActivitySourceServiceDeleteEvent(KubernetesActivitySource kubernetesActivitySource) {
    Set<KubernetesActivitySourceConfig> activitySourceConfigs = kubernetesActivitySource.getActivitySourceConfigs();

    String serviceIdentifier = null;
    if (hasSome(activitySourceConfigs)) {
      for (KubernetesActivitySourceConfig config : activitySourceConfigs) {
        serviceIdentifier = config.getServiceIdentifier();
      }
    }
    IdentifierRef identifierRef = getIdentifierRefKubernetesSource(kubernetesActivitySource, serviceIdentifier);
    String kubernetesActivitySourceServiceFQN = getFullyQualifiedIdentifierFromIdentifierRef(identifierRef);
    String kubernetesActivitySourceFQN = getKubernetesActivitySourceFullyQualifiedName(
        kubernetesActivitySource, kubernetesActivitySource.getIdentifier());
    DeleteSetupUsageDTO deleteSetupUsageDTO = getDeleteSetupUsageDTO(kubernetesActivitySource,
        EntityTypeProtoEnum.SERVICE, kubernetesActivitySourceServiceFQN, kubernetesActivitySourceFQN);

    sendEventWithMessageForDeletion(kubernetesActivitySource, deleteSetupUsageDTO);
  }

  @Override
  public void sendKubernetesActivitySourceEnvironmentCreateEvent(KubernetesActivitySource kubernetesActivitySource) {
    Set<KubernetesActivitySourceConfig> activitySourceConfigs = kubernetesActivitySource.getActivitySourceConfigs();

    String environmentIdentifier = null;
    if (hasSome(activitySourceConfigs)) {
      for (KubernetesActivitySourceConfig config : activitySourceConfigs) {
        environmentIdentifier = config.getEnvIdentifier();
      }
    }
    IdentifierRefProtoDTO kubernetesActivitySourceReference =
        getIdentifierRefProtoDTOFromKubernetesActivitySource(kubernetesActivitySource);
    IdentifierRef identifierRef = getIdentifierRefKubernetesSource(kubernetesActivitySource, environmentIdentifier);
    IdentifierRefProtoDTO kubernetesActivitySourceReferenceEnvironment =
        getIdentifierRefProtoDTOFromIdentifierRef(identifierRef);
    EntitySetupUsageCreateDTO environmentEntityReferenceDTO =
        getEntitySetupUsageCreateDTO(kubernetesActivitySource, kubernetesActivitySourceReference,
            kubernetesActivitySourceReferenceEnvironment, EntityTypeProtoEnum.ENVIRONMENT);

    sendEventWithMessageForCreation(kubernetesActivitySource, environmentEntityReferenceDTO);
  }

  @Override
  public void sendKubernetesActivitySourceEnvironmentDeleteEvent(KubernetesActivitySource kubernetesActivitySource) {
    Set<KubernetesActivitySourceConfig> activitySourceConfigs = kubernetesActivitySource.getActivitySourceConfigs();

    String environmentIdentifier = null;
    if (hasSome(activitySourceConfigs)) {
      for (KubernetesActivitySourceConfig config : activitySourceConfigs) {
        environmentIdentifier = config.getEnvIdentifier();
      }
    }
    IdentifierRef identifierRef = getIdentifierRefKubernetesSource(kubernetesActivitySource, environmentIdentifier);
    String kubernetesActivitySourceEnvironmentFQN = getFullyQualifiedIdentifierFromIdentifierRef(identifierRef);
    String kubernetesActivitySourceFQN = getKubernetesActivitySourceFullyQualifiedName(
        kubernetesActivitySource, kubernetesActivitySource.getIdentifier());

    DeleteSetupUsageDTO deleteSetupUsageDTO = getDeleteSetupUsageDTO(kubernetesActivitySource,
        EntityTypeProtoEnum.ENVIRONMENT, kubernetesActivitySourceEnvironmentFQN, kubernetesActivitySourceFQN);

    sendEventWithMessageForDeletion(kubernetesActivitySource, deleteSetupUsageDTO);
  }

  private void sendEventWithMessageForCreation(
      CVConfig cvConfig, EntitySetupUsageCreateDTO connectorEntityReferenceDTO) {
    sendMessage(
        Message.newBuilder()
            .putAllMetadata(ImmutableMap.of("accountId", cvConfig.getAccountId(),
                EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.SETUP_USAGE_ENTITY,
                EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.CREATE_ACTION))
            .setData(connectorEntityReferenceDTO.toByteString())
            .build(),
        cvConfig);
  }

  private void sendEventWithMessageForCreation(
      KubernetesActivitySource kubernetesActivitySource, EntitySetupUsageCreateDTO connectorEntityReferenceDTO) {
    sendMessage(
        Message.newBuilder()
            .putAllMetadata(ImmutableMap.of("accountId", kubernetesActivitySource.getAccountId(),
                EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.SETUP_USAGE_ENTITY,
                EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.CREATE_ACTION))
            .setData(connectorEntityReferenceDTO.toByteString())
            .build(),
        kubernetesActivitySource);
  }

  private void sendEventWithMessageForDeletion(CVConfig cvConfig, DeleteSetupUsageDTO deleteSetupUsageDTO) {
    sendMessage(
        Message.newBuilder()
            .putAllMetadata(ImmutableMap.of("accountId", cvConfig.getAccountId(),
                EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.SETUP_USAGE_ENTITY,
                EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.DELETE_ACTION))
            .setData(deleteSetupUsageDTO.toByteString())
            .build(),
        cvConfig);
  }

  private void sendEventWithMessageForDeletion(
      VerificationJob verificationJob, DeleteSetupUsageDTO deleteSetupUsageDTO) {
    sendMessage(
        Message.newBuilder()
            .putAllMetadata(ImmutableMap.of("accountId", verificationJob.getAccountId(),
                EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.SETUP_USAGE_ENTITY,
                EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.DELETE_ACTION))
            .setData(deleteSetupUsageDTO.toByteString())
            .build(),
        verificationJob);
  }

  private void sendEventWithMessageForDeletion(
      KubernetesActivitySource kubernetesActivitySource, DeleteSetupUsageDTO deleteSetupUsageDTO) {
    sendMessage(
        Message.newBuilder()
            .putAllMetadata(ImmutableMap.of("accountId", kubernetesActivitySource.getAccountId(),
                EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.SETUP_USAGE_ENTITY,
                EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.DELETE_ACTION))
            .setData(deleteSetupUsageDTO.toByteString())
            .build(),
        kubernetesActivitySource);
  }

  private void sendEventWithMessageForCreation(
      VerificationJob verificationJob, EntitySetupUsageCreateDTO connectorEntityReferenceDTO) {
    sendMessage(
        Message.newBuilder()
            .putAllMetadata(ImmutableMap.of("accountId", verificationJob.getAccountId(),
                EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.SETUP_USAGE_ENTITY,
                EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.CREATE_ACTION))
            .setData(connectorEntityReferenceDTO.toByteString())
            .build(),
        verificationJob);
  }

  private <T> void sendMessage(Message message, T entityToLog) {
    try {
      eventProducer.send(message);
    } catch (Exception e) {
      log.error(ENTITY_REFERENCE_LOG_PREFIX + " Error while sending referenced Object: {}, Message: {}", entityToLog,
          message);
      throw new IllegalStateException(e);
    }
  }

  private IdentifierRefProtoDTO getIdentifierRefProtoDTOFromIdentifierRef(IdentifierRef identifierRef) {
    return identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(identifierRef.getAccountIdentifier(),
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
  }

  private IdentifierRefProtoDTO getIdentifierRefProtoDTOFromConfig(CVConfig cvConfig) {
    return identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(cvConfig.getAccountId(), cvConfig.getOrgIdentifier(),
        cvConfig.getProjectIdentifier(), cvConfig.getIdentifier());
  }

  private IdentifierRefProtoDTO getIdentifierRefProtoDTOFromVerificationJob(VerificationJob verificationJob) {
    return identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(verificationJob.getAccountId(),
        verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier(), verificationJob.getIdentifier());
  }

  private IdentifierRefProtoDTO getIdentifierRefProtoDTOFromKubernetesActivitySource(
      KubernetesActivitySource kubernetesActivitySource) {
    return identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(kubernetesActivitySource.getAccountId(),
        kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier(),
        kubernetesActivitySource.getIdentifier());
  }

  private IdentifierRef getIdentifierRef(CVConfig cvConfig, String scopedIdentifier) {
    return IdentifierRefHelper.getIdentifierRef(
        scopedIdentifier, cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier());
  }

  private IdentifierRef getIdentifierRefVerificationJob(VerificationJob verificationJob, String scopedIdentifier) {
    return IdentifierRefHelper.getIdentifierRef(scopedIdentifier, verificationJob.getAccountId(),
        verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier());
  }

  private IdentifierRef getIdentifierRefKubernetesSource(
      KubernetesActivitySource kubernetesActivitySource, String scopedIdentifier) {
    return IdentifierRefHelper.getIdentifierRef(scopedIdentifier, kubernetesActivitySource.getAccountId(),
        kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier());
  }

  private String getCVConfigFullyQualifiedName(CVConfig cvConfig, String scopedIdentifier) {
    return FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(), scopedIdentifier);
  }

  private String getVerificationJobFullyQualifiedName(VerificationJob verificationJob, String scopedIdentifier) {
    return FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(verificationJob.getAccountId(),
        verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier(), scopedIdentifier);
  }

  private String getKubernetesActivitySourceFullyQualifiedName(
      KubernetesActivitySource kubernetesActivitySource, String scopedIdentifier) {
    return FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(kubernetesActivitySource.getAccountId(),
        kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier(), scopedIdentifier);
  }

  private String getFullyQualifiedIdentifierFromIdentifierRef(IdentifierRef identifierRef) {
    return FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(identifierRef.getAccountIdentifier(),
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
  }

  @NotNull
  private DeleteSetupUsageDTO getDeleteSetupUsageDTO(
      CVConfig cvConfig, String cvConfigScopedFQN, EntityTypeProtoEnum cvConfigScopedType, String cvConfigFQN) {
    return DeleteSetupUsageDTO.newBuilder()
        .setAccountIdentifier(cvConfig.getAccountId())
        .setReferredByEntityFQN(cvConfigFQN)
        .setReferredByEntityType(EntityTypeProtoEnum.CV_CONFIG)
        .setReferredEntityFQN(cvConfigScopedFQN)
        .setReferredEntityType(cvConfigScopedType)
        .build();
  }

  @NotNull
  private DeleteSetupUsageDTO getDeleteSetupUsageDTO(VerificationJob verificationJob, String verificationJobScopedFQN,
      EntityTypeProtoEnum verificationJobEntityType, String verificationJobFQN) {
    return DeleteSetupUsageDTO.newBuilder()
        .setAccountIdentifier(verificationJob.getAccountId())
        .setReferredByEntityFQN(verificationJobFQN)
        .setReferredByEntityType(EntityTypeProtoEnum.CV_VERIFICATION_JOB)
        .setReferredEntityFQN(verificationJobScopedFQN)
        .setReferredEntityType(verificationJobEntityType)
        .build();
  }

  private DeleteSetupUsageDTO getDeleteSetupUsageDTO(KubernetesActivitySource kubernetesActivitySource,
      EntityTypeProtoEnum kubernetesActivitySourceScopedEntityType, String kubernetesActivitySourceScopedFQN,
      String kubernetesActivitySourceFQN) {
    return DeleteSetupUsageDTO.newBuilder()
        .setAccountIdentifier(kubernetesActivitySource.getAccountId())
        .setReferredByEntityFQN(kubernetesActivitySourceFQN)
        .setReferredByEntityType(EntityTypeProtoEnum.CV_KUBERNETES_ACTIVITY_SOURCE)
        .setReferredEntityFQN(kubernetesActivitySourceScopedFQN)
        .setReferredEntityType(kubernetesActivitySourceScopedEntityType)
        .build();
  }

  @NotNull
  private EntitySetupUsageCreateDTO getEntitySetupUsageCreateDTO(CVConfig cvConfig,
      IdentifierRefProtoDTO configReference, IdentifierRefProtoDTO configReferenceEnvironment,
      EntityTypeProtoEnum typeProtoEnum) {
    EntityDetailProtoDTO configDetails = EntityDetailProtoDTO.newBuilder()
                                             .setIdentifierRef(configReference)
                                             .setType(EntityTypeProtoEnum.CV_CONFIG)
                                             .setName(cvConfig.getMonitoringSourceName())
                                             .build();

    EntityDetailProtoDTO scopedManagerDetails =
        EntityDetailProtoDTO.newBuilder().setIdentifierRef(configReferenceEnvironment).setType(typeProtoEnum).build();

    return EntitySetupUsageCreateDTO.newBuilder()
        .setAccountIdentifier(cvConfig.getAccountId())
        .setReferredByEntity(configDetails)
        .setReferredEntity(scopedManagerDetails)
        .build();
  }

  @NotNull
  private EntitySetupUsageCreateDTO getEntitySetupUsageCreateDTO(VerificationJob verificationJob,
      IdentifierRefProtoDTO verificationJobReference, IdentifierRefProtoDTO verificationJobReferenceEnvironment,
      EntityTypeProtoEnum typeProtoEnum) {
    EntityDetailProtoDTO verificationJobDetails = EntityDetailProtoDTO.newBuilder()
                                                      .setIdentifierRef(verificationJobReference)
                                                      .setType(EntityTypeProtoEnum.CV_VERIFICATION_JOB)
                                                      .setName(verificationJob.getJobName())
                                                      .build();

    EntityDetailProtoDTO scopedManagerDetails = EntityDetailProtoDTO.newBuilder()
                                                    .setIdentifierRef(verificationJobReferenceEnvironment)
                                                    .setType(typeProtoEnum)
                                                    .build();

    return EntitySetupUsageCreateDTO.newBuilder()
        .setAccountIdentifier(verificationJob.getAccountId())
        .setReferredByEntity(verificationJobDetails)
        .setReferredEntity(scopedManagerDetails)
        .build();
  }

  private EntitySetupUsageCreateDTO getEntitySetupUsageCreateDTO(KubernetesActivitySource kubernetesActivitySource,
      IdentifierRefProtoDTO kubernetesActivitySourceReference,
      IdentifierRefProtoDTO kubernetesActivitySourceReferenceEnvironment, EntityTypeProtoEnum typeProtoEnum) {
    EntityDetailProtoDTO kubernetesActivitySourceDetails =
        EntityDetailProtoDTO.newBuilder()
            .setIdentifierRef(kubernetesActivitySourceReference)
            .setType(EntityTypeProtoEnum.CV_KUBERNETES_ACTIVITY_SOURCE)
            .setName(kubernetesActivitySource.getName())
            .build();

    EntityDetailProtoDTO scopedManagerDetails = EntityDetailProtoDTO.newBuilder()
                                                    .setIdentifierRef(kubernetesActivitySourceReferenceEnvironment)
                                                    .setType(typeProtoEnum)
                                                    .build();

    return EntitySetupUsageCreateDTO.newBuilder()
        .setAccountIdentifier(kubernetesActivitySource.getAccountId())
        .setReferredByEntity(kubernetesActivitySourceDetails)
        .setReferredEntity(scopedManagerDetails)
        .build();
  }
}
