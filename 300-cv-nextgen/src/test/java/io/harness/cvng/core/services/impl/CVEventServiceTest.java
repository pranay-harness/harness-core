package io.harness.cvng.core.services.impl;

import static io.harness.EntityType.CONNECTORS;
import static io.harness.EntityType.CV_CONFIG;
import static io.harness.EntityType.CV_KUBERNETES_ACTIVITY_SOURCE;
import static io.harness.EntityType.CV_VERIFICATION_JOB;
import static io.harness.EntityType.ENVIRONMENT;
import static io.harness.EntityType.SERVICE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VUK;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO.KubernetesActivitySourceConfig;
import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.beans.job.TestVerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entitysetupusage.DeleteSetupUsageDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.rule.Owner;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class CVEventServiceTest extends CvNextGenTestBase {
  @Mock private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;
  @Mock private AbstractProducer eventProducer;

  @InjectMocks private CVEventServiceImpl eventService;
  @Inject private VerificationJobService verificationJobService;
  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendConnectorCreateEvent() throws InvalidProtocolBufferException {
    CVConfig cvConfig = createCVConfig();

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(cvConfig.getConnectorIdentifier(),
        cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier());

    String accountIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String projectIdentifier = generateUuid();
    String identifier = generateUuid();

    IdentifierRefProtoDTO configReference =
        getIdentifierRefProtoDTO(accountIdentifier, identifier, projectIdentifier, orgIdentifier);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(cvConfig.getAccountId(), cvConfig.getOrgIdentifier(),
             cvConfig.getProjectIdentifier(), cvConfig.getIdentifier()))
        .thenReturn(configReference);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(identifierRef.getAccountIdentifier(),
             identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier()))
        .thenReturn(configReference);

    eventService.sendConnectorCreateEvent(cvConfig);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);

    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    EntitySetupUsageCreateDTO entityReferenceDTO =
        EntitySetupUsageCreateDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(entityReferenceDTO).isNotNull();
    assertThat(entityReferenceDTO.getReferredEntity().getType().toString()).isEqualTo(CONNECTORS.name());
    assertThat(entityReferenceDTO.getReferredByEntity().getType().toString()).isEqualTo(CV_CONFIG.name());
    assertThat(entityReferenceDTO.getAccountIdentifier()).isEqualTo(cvConfig.getAccountId());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendConnectorDeleteEvent() throws InvalidProtocolBufferException {
    CVConfig cvConfig = createCVConfig();

    eventService.sendConnectorDeleteEvent(cvConfig);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);

    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    DeleteSetupUsageDTO deleteSetupUsageDTO = DeleteSetupUsageDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(deleteSetupUsageDTO).isNotNull();
    assertThat(deleteSetupUsageDTO.getAccountIdentifier()).isEqualTo(cvConfig.getAccountId());
    assertThat(deleteSetupUsageDTO.getReferredByEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(cvConfig.getAccountId(),
            cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(), cvConfig.getIdentifier()));
    assertThat(deleteSetupUsageDTO.getReferredEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(cvConfig.getAccountId(),
            cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(), cvConfig.getConnectorIdentifier()));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendServiceCreateEvent() throws InvalidProtocolBufferException {
    CVConfig cvConfig = createCVConfig();

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(cvConfig.getServiceIdentifier(),
        cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier());

    String accountIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String projectIdentifier = generateUuid();
    String identifier = generateUuid();

    IdentifierRefProtoDTO configReference =
        getIdentifierRefProtoDTO(accountIdentifier, identifier, projectIdentifier, orgIdentifier);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(cvConfig.getAccountId(), cvConfig.getOrgIdentifier(),
             cvConfig.getProjectIdentifier(), cvConfig.getIdentifier()))
        .thenReturn(configReference);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(identifierRef.getAccountIdentifier(),
             identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier()))
        .thenReturn(configReference);

    eventService.sendServiceCreateEvent(cvConfig);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);

    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    EntitySetupUsageCreateDTO entityReferenceDTO =
        EntitySetupUsageCreateDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(entityReferenceDTO).isNotNull();
    assertThat(entityReferenceDTO.getReferredEntity().getType().toString()).isEqualTo(SERVICE.name());
    assertThat(entityReferenceDTO.getReferredByEntity().getType().toString()).isEqualTo(CV_CONFIG.name());
    assertThat(entityReferenceDTO.getAccountIdentifier()).isEqualTo(cvConfig.getAccountId());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendServiceDeleteEvent() throws InvalidProtocolBufferException {
    CVConfig cvConfig = createCVConfig();

    eventService.sendServiceDeleteEvent(cvConfig);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);

    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    DeleteSetupUsageDTO deleteSetupUsageDTO = DeleteSetupUsageDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(deleteSetupUsageDTO).isNotNull();
    assertThat(deleteSetupUsageDTO.getAccountIdentifier()).isEqualTo(cvConfig.getAccountId());
    assertThat(deleteSetupUsageDTO.getReferredByEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(cvConfig.getAccountId(),
            cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(), cvConfig.getIdentifier()));
    assertThat(deleteSetupUsageDTO.getReferredEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(cvConfig.getAccountId(),
            cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(), cvConfig.getServiceIdentifier()));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendEnvironmentCreateEvent() throws InvalidProtocolBufferException {
    CVConfig cvConfig = createCVConfig();

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(cvConfig.getEnvIdentifier(),
        cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier());

    String accountIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String projectIdentifier = generateUuid();
    String identifier = generateUuid();

    IdentifierRefProtoDTO configReference =
        getIdentifierRefProtoDTO(accountIdentifier, identifier, projectIdentifier, orgIdentifier);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(cvConfig.getAccountId(), cvConfig.getOrgIdentifier(),
             cvConfig.getProjectIdentifier(), cvConfig.getIdentifier()))
        .thenReturn(configReference);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(identifierRef.getAccountIdentifier(),
             identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier()))
        .thenReturn(configReference);

    eventService.sendEnvironmentCreateEvent(cvConfig);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);

    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    EntitySetupUsageCreateDTO entityReferenceDTO =
        EntitySetupUsageCreateDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(entityReferenceDTO).isNotNull();
    assertThat(entityReferenceDTO.getReferredEntity().getType().toString()).isEqualTo(ENVIRONMENT.name());
    assertThat(entityReferenceDTO.getReferredByEntity().getType().toString()).isEqualTo(CV_CONFIG.name());
    assertThat(entityReferenceDTO.getAccountIdentifier()).isEqualTo(cvConfig.getAccountId());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendEnvironmentDeleteEvent() throws InvalidProtocolBufferException {
    CVConfig cvConfig = createCVConfig();

    eventService.sendEnvironmentDeleteEvent(cvConfig);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);

    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    DeleteSetupUsageDTO deleteSetupUsageDTO = DeleteSetupUsageDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(deleteSetupUsageDTO).isNotNull();
    assertThat(deleteSetupUsageDTO.getAccountIdentifier()).isEqualTo(cvConfig.getAccountId());
    assertThat(deleteSetupUsageDTO.getReferredByEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(cvConfig.getAccountId(),
            cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(), cvConfig.getIdentifier()));
    assertThat(deleteSetupUsageDTO.getReferredEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(cvConfig.getAccountId(),
            cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(), cvConfig.getEnvIdentifier()));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendVerificationJobEnvironmentCreateEvent() throws InvalidProtocolBufferException {
    String accountId = generateUuid();
    String accountIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String projectIdentifier = generateUuid();
    String identifier = generateUuid();

    VerificationJob verificationJob = verificationJobService.fromDto(createVerificationJobDTOWithoutRuntimeParams());
    verificationJob.setAccountId(accountId);

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(verificationJob.getEnvIdentifier(),
        verificationJob.getAccountId(), verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier());

    IdentifierRefProtoDTO verificationJobReference =
        getIdentifierRefProtoDTO(accountIdentifier, identifier, projectIdentifier, orgIdentifier);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(verificationJob.getAccountId(),
             verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier(),
             verificationJob.getIdentifier()))
        .thenReturn(verificationJobReference);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(identifierRef.getAccountIdentifier(),
             identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier()))
        .thenReturn(verificationJobReference);

    eventService.sendVerificationJobEnvironmentCreateEvent(verificationJob);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    EntitySetupUsageCreateDTO entityReferenceDTO =
        EntitySetupUsageCreateDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(entityReferenceDTO).isNotNull();
    assertThat(entityReferenceDTO.getReferredEntity().getType().toString()).isEqualTo(ENVIRONMENT.name());
    assertThat(entityReferenceDTO.getReferredByEntity().getType().toString()).isEqualTo(CV_VERIFICATION_JOB.name());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendVerificationJobServiceCreateEvent() throws InvalidProtocolBufferException {
    String accountId = generateUuid();
    String accountIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String projectIdentifier = generateUuid();
    String identifier = generateUuid();

    VerificationJob verificationJob = verificationJobService.fromDto(createVerificationJobDTOWithoutRuntimeParams());
    verificationJob.setAccountId(accountId);

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(verificationJob.getServiceIdentifier(),
        verificationJob.getAccountId(), verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier());

    IdentifierRefProtoDTO verificationJobReference =
        getIdentifierRefProtoDTO(accountIdentifier, identifier, projectIdentifier, orgIdentifier);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(verificationJob.getAccountId(),
             verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier(),
             verificationJob.getIdentifier()))
        .thenReturn(verificationJobReference);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(identifierRef.getAccountIdentifier(),
             identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier()))
        .thenReturn(verificationJobReference);

    eventService.sendVerificationJobServiceCreateEvent(verificationJob);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    EntitySetupUsageCreateDTO entityReferenceDTO =
        EntitySetupUsageCreateDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(entityReferenceDTO).isNotNull();
    assertThat(entityReferenceDTO.getReferredEntity().getType().toString()).isEqualTo(SERVICE.name());
    assertThat(entityReferenceDTO.getReferredByEntity().getType().toString()).isEqualTo(CV_VERIFICATION_JOB.name());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendKubernetesActivitySourceConnectorCreateEvent() throws InvalidProtocolBufferException {
    String accountId = generateUuid();
    String identifier = generateUuid();
    String projectIdentifier = generateUuid();
    String orgIdentifier = generateUuid();

    KubernetesActivitySource kubernetesActivitySource =
        getKubernetesActivitySource(accountId, identifier, projectIdentifier, orgIdentifier, emptySet());

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        kubernetesActivitySource.getConnectorIdentifier(), kubernetesActivitySource.getAccountId(),
        kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier());

    IdentifierRefProtoDTO configReference =
        getIdentifierRefProtoDTO(accountId, identifier, projectIdentifier, orgIdentifier);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(kubernetesActivitySource.getAccountId(),
             kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier(),
             kubernetesActivitySource.getIdentifier()))
        .thenReturn(configReference);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(identifierRef.getAccountIdentifier(),
             identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier()))
        .thenReturn(configReference);

    eventService.sendKubernetesActivitySourceConnectorCreateEvent(kubernetesActivitySource);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    EntitySetupUsageCreateDTO entityReferenceDTO =
        EntitySetupUsageCreateDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(entityReferenceDTO.getReferredEntity().getType().toString()).isEqualTo(CONNECTORS.name());
    assertThat(entityReferenceDTO.getReferredByEntity().getType().toString())
        .isEqualTo(CV_KUBERNETES_ACTIVITY_SOURCE.name());
    assertThat(entityReferenceDTO.getAccountIdentifier()).isEqualTo(kubernetesActivitySource.getAccountId());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendKubernetesActivitySourceServiceCreateEvent() throws InvalidProtocolBufferException {
    String accountId = generateUuid();
    String identifier = generateUuid();
    String projectIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String serviceIdentifier = generateUuid();

    Set<KubernetesActivitySourceConfig> activitySourceConfigs =
        Stream.of(KubernetesActivitySourceConfig.builder().serviceIdentifier(serviceIdentifier).build())
            .collect(Collectors.toSet());

    KubernetesActivitySource kubernetesActivitySource =
        getKubernetesActivitySource(accountId, identifier, projectIdentifier, orgIdentifier, activitySourceConfigs);

    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef(serviceIdentifier, kubernetesActivitySource.getAccountId(),
            kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier());

    IdentifierRefProtoDTO configReference =
        getIdentifierRefProtoDTO(accountId, identifier, projectIdentifier, orgIdentifier);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(kubernetesActivitySource.getAccountId(),
             kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier(),
             kubernetesActivitySource.getIdentifier()))
        .thenReturn(configReference);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(identifierRef.getAccountIdentifier(),
             identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier()))
        .thenReturn(configReference);

    eventService.sendKubernetesActivitySourceServiceCreateEvent(kubernetesActivitySource);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    EntitySetupUsageCreateDTO entityReferenceDTO =
        EntitySetupUsageCreateDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(entityReferenceDTO.getReferredEntity().getType().toString()).isEqualTo(SERVICE.name());
    assertThat(entityReferenceDTO.getReferredByEntity().getType().toString())
        .isEqualTo(CV_KUBERNETES_ACTIVITY_SOURCE.name());
    assertThat(entityReferenceDTO.getAccountIdentifier()).isEqualTo(kubernetesActivitySource.getAccountId());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendKubernetesActivitySourceEnvironmentCreateEvent() throws InvalidProtocolBufferException {
    String accountId = generateUuid();
    String identifier = generateUuid();
    String projectIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String environmentIdentifier = generateUuid();

    Set<KubernetesActivitySourceConfig> activitySourceConfigs =
        Stream.of(KubernetesActivitySourceConfig.builder().envIdentifier(environmentIdentifier).build())
            .collect(Collectors.toSet());

    KubernetesActivitySource kubernetesActivitySource =
        getKubernetesActivitySource(accountId, identifier, projectIdentifier, orgIdentifier, activitySourceConfigs);

    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef(environmentIdentifier, kubernetesActivitySource.getAccountId(),
            kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier());

    IdentifierRefProtoDTO configReference =
        getIdentifierRefProtoDTO(accountId, identifier, projectIdentifier, orgIdentifier);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(kubernetesActivitySource.getAccountId(),
             kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier(),
             kubernetesActivitySource.getIdentifier()))
        .thenReturn(configReference);

    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(identifierRef.getAccountIdentifier(),
             identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier()))
        .thenReturn(configReference);

    eventService.sendKubernetesActivitySourceEnvironmentCreateEvent(kubernetesActivitySource);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    EntitySetupUsageCreateDTO entityReferenceDTO =
        EntitySetupUsageCreateDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(entityReferenceDTO.getReferredEntity().getType().toString()).isEqualTo(ENVIRONMENT.name());
    assertThat(entityReferenceDTO.getReferredByEntity().getType().toString())
        .isEqualTo(CV_KUBERNETES_ACTIVITY_SOURCE.name());
    assertThat(entityReferenceDTO.getAccountIdentifier()).isEqualTo(kubernetesActivitySource.getAccountId());
  }

  @NotNull
  private IdentifierRefProtoDTO getIdentifierRefProtoDTO(
      String accountId, String identifier, String projectIdentifier, String orgIdentifier) {
    return IdentifierRefProtoDTO.newBuilder()
        .setAccountIdentifier(StringValue.of(accountId))
        .setOrgIdentifier(StringValue.of(orgIdentifier))
        .setProjectIdentifier(StringValue.of(projectIdentifier))
        .setIdentifier(StringValue.of(identifier))
        .build();
  }

  private KubernetesActivitySource getKubernetesActivitySource(String accountId, String identifier,
      String projectIdentifier, String orgIdentifier, Set<KubernetesActivitySourceConfig> activitySourceConfigs) {
    return KubernetesActivitySource.builder()
        .accountId(accountId)
        .identifier(identifier)
        .name("testName")
        .projectIdentifier(projectIdentifier)
        .orgIdentifier(orgIdentifier)
        .connectorIdentifier(generateUuid())
        .activitySourceConfigs(activitySourceConfigs)
        .build();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendVerificationJobEnvironmentDeleteEvent() throws InvalidProtocolBufferException {
    String accountId = generateUuid();
    VerificationJob verificationJob = verificationJobService.fromDto(createVerificationJobDTOWithoutRuntimeParams());
    verificationJob.setAccountId(accountId);

    eventService.sendVerificationJobEnvironmentDeleteEvent(verificationJob);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    DeleteSetupUsageDTO deleteSetupUsageDTO = DeleteSetupUsageDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(deleteSetupUsageDTO).isNotNull();
    assertThat(deleteSetupUsageDTO.getAccountIdentifier()).isEqualTo(verificationJob.getAccountId());
    assertThat(deleteSetupUsageDTO.getReferredByEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(verificationJob.getAccountId(),
            verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier(),
            verificationJob.getIdentifier()));
    assertThat(deleteSetupUsageDTO.getReferredEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(verificationJob.getAccountId(),
            verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier(),
            verificationJob.getEnvIdentifier()));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendVerificationJobServiceDeleteEvent() throws InvalidProtocolBufferException {
    String accountId = generateUuid();
    VerificationJob verificationJob = verificationJobService.fromDto(createVerificationJobDTOWithoutRuntimeParams());
    verificationJob.setAccountId(accountId);

    eventService.sendVerificationJobServiceDeleteEvent(verificationJob);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    DeleteSetupUsageDTO deleteSetupUsageDTO = DeleteSetupUsageDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(deleteSetupUsageDTO.getAccountIdentifier()).isEqualTo(verificationJob.getAccountId());
    assertThat(deleteSetupUsageDTO.getReferredByEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(verificationJob.getAccountId(),
            verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier(),
            verificationJob.getIdentifier()));
    assertThat(deleteSetupUsageDTO.getReferredEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(verificationJob.getAccountId(),
            verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier(),
            verificationJob.getServiceIdentifier()));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendKubernetesActivitySourceEnvironmentDeleteEvent() throws InvalidProtocolBufferException {
    String accountId = generateUuid();
    String identifier = generateUuid();
    String projectIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String envIdentifier = generateUuid();

    Set<KubernetesActivitySourceConfig> activitySourceConfigs =
        Stream.of(KubernetesActivitySourceConfig.builder().envIdentifier(envIdentifier).build())
            .collect(Collectors.toSet());

    KubernetesActivitySource kubernetesActivitySource =
        getKubernetesActivitySource(accountId, identifier, projectIdentifier, orgIdentifier, activitySourceConfigs);

    eventService.sendKubernetesActivitySourceEnvironmentDeleteEvent(kubernetesActivitySource);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    DeleteSetupUsageDTO deleteSetupUsageDTO = DeleteSetupUsageDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(deleteSetupUsageDTO).isNotNull();
    assertThat(deleteSetupUsageDTO.getAccountIdentifier()).isEqualTo(kubernetesActivitySource.getAccountId());
    assertThat(deleteSetupUsageDTO.getReferredByEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(kubernetesActivitySource.getAccountId(),
            kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier(),
            kubernetesActivitySource.getIdentifier()));
    assertThat(deleteSetupUsageDTO.getReferredEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(kubernetesActivitySource.getAccountId(),
            kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier(),
            envIdentifier));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendKubernetesActivitySourceServiceDeleteEvent() throws InvalidProtocolBufferException {
    String accountId = generateUuid();
    String identifier = generateUuid();
    String projectIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String serviceIdentifier = generateUuid();

    Set<KubernetesActivitySourceConfig> activitySourceConfigs =
        Stream.of(KubernetesActivitySourceConfig.builder().serviceIdentifier(serviceIdentifier).build())
            .collect(Collectors.toSet());

    KubernetesActivitySource kubernetesActivitySource =
        getKubernetesActivitySource(accountId, identifier, projectIdentifier, orgIdentifier, activitySourceConfigs);

    eventService.sendKubernetesActivitySourceServiceDeleteEvent(kubernetesActivitySource);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    DeleteSetupUsageDTO deleteSetupUsageDTO = DeleteSetupUsageDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(deleteSetupUsageDTO).isNotNull();
    assertThat(deleteSetupUsageDTO.getAccountIdentifier()).isEqualTo(kubernetesActivitySource.getAccountId());
    assertThat(deleteSetupUsageDTO.getReferredByEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(kubernetesActivitySource.getAccountId(),
            kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier(),
            kubernetesActivitySource.getIdentifier()));
    assertThat(deleteSetupUsageDTO.getReferredEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(kubernetesActivitySource.getAccountId(),
            kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier(),
            serviceIdentifier));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendKubernetesActivitySourceConnectorDeleteEvent() throws InvalidProtocolBufferException {
    String accountId = generateUuid();
    String identifier = generateUuid();
    String projectIdentifier = generateUuid();
    String orgIdentifier = generateUuid();

    KubernetesActivitySource kubernetesActivitySource =
        getKubernetesActivitySource(accountId, identifier, projectIdentifier, orgIdentifier, emptySet());

    eventService.sendKubernetesActivitySourceConnectorDeleteEvent(kubernetesActivitySource);

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(eventProducer, times(1)).send(argumentCaptor.capture());

    DeleteSetupUsageDTO deleteSetupUsageDTO = DeleteSetupUsageDTO.parseFrom(argumentCaptor.getValue().getData());

    assertThat(deleteSetupUsageDTO).isNotNull();

    assertThat(deleteSetupUsageDTO.getAccountIdentifier()).isEqualTo(kubernetesActivitySource.getAccountId());
    assertThat(deleteSetupUsageDTO.getReferredByEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(kubernetesActivitySource.getAccountId(),
            kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier(),
            kubernetesActivitySource.getIdentifier()));
    assertThat(deleteSetupUsageDTO.getReferredEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(kubernetesActivitySource.getAccountId(),
            kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier(),
            kubernetesActivitySource.getConnectorIdentifier()));
  }

  private CVConfig createCVConfig() {
    String serviceInstanceIdentifier = generateUuid();

    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillValues(cvConfig);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier(serviceInstanceIdentifier);
    return cvConfig;
  }

  private void fillValues(CVConfig cvConfig) {
    String accountId = generateUuid();
    String connectorIdentifier = generateUuid();
    String productName = generateUuid();
    String groupId = generateUuid();
    String projectIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String monitoringSourceIdentifier = generateUuid();
    String monitoringSourceName = generateUuid();

    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorIdentifier(connectorIdentifier);
    cvConfig.setServiceIdentifier("service");
    cvConfig.setEnvIdentifier("env");
    cvConfig.setOrgIdentifier(orgIdentifier);
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setIdentifier(groupId);
    cvConfig.setMonitoringSourceName(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(productName);
    cvConfig.setIdentifier(monitoringSourceIdentifier);
    cvConfig.setMonitoringSourceName(monitoringSourceName);
  }

  private VerificationJobDTO createVerificationJobDTO() {
    TestVerificationJobDTO testVerificationJobDTO = new TestVerificationJobDTO();
    testVerificationJobDTO.setActivitySourceIdentifier(generateUuid());
    testVerificationJobDTO.setIdentifier(generateUuid());
    testVerificationJobDTO.setJobName(generateUuid());
    testVerificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS));
    testVerificationJobDTO.setBaselineVerificationJobInstanceId(null);
    testVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    testVerificationJobDTO.setServiceIdentifier(generateUuid());
    testVerificationJobDTO.setEnvIdentifier(generateUuid());
    testVerificationJobDTO.setBaselineVerificationJobInstanceId(generateUuid());
    testVerificationJobDTO.setDuration("15m");
    testVerificationJobDTO.setProjectIdentifier(generateUuid());
    testVerificationJobDTO.setOrgIdentifier(generateUuid());
    return testVerificationJobDTO;
  }

  private VerificationJobDTO createVerificationJobDTOWithoutRuntimeParams() {
    TestVerificationJobDTO testVerificationJobDTO = (TestVerificationJobDTO) createVerificationJobDTO();
    testVerificationJobDTO.setEnvIdentifier("testEnv");
    testVerificationJobDTO.setServiceIdentifier("testSer");
    testVerificationJobDTO.setJobName("job-Name");
    return testVerificationJobDTO;
  }
}
