package io.harness.cvng.core.services.impl;

import static io.harness.EntityType.CONNECTORS;
import static io.harness.EntityType.CV_CONFIG;
import static io.harness.EntityType.CV_VERIFICATION_JOB;
import static io.harness.EntityType.ENVIRONMENT;
import static io.harness.EntityType.SERVICE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTest;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.TestVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entitysetupusage.DeleteSetupUsageDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.rule.Owner;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class CVEventServiceTest extends CvNextGenTest {
  @Mock private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;
  @Mock private AbstractProducer eventProducer;

  @InjectMocks private CVEventServiceImpl eventService;

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSendConnectorCreateEvent() throws ProducerShutdownException, InvalidProtocolBufferException {
    CVConfig cvConfig = createCVConfig();

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(cvConfig.getConnectorIdentifier(),
        cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier());

    String accountIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String projectIdentifier = generateUuid();
    String identifier = generateUuid();

    IdentifierRefProtoDTO configReference = IdentifierRefProtoDTO.newBuilder()
                                                .setAccountIdentifier(StringValue.of(accountIdentifier))
                                                .setOrgIdentifier(StringValue.of(orgIdentifier))
                                                .setProjectIdentifier(StringValue.of(projectIdentifier))
                                                .setIdentifier(StringValue.of(identifier))
                                                .build();

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
  public void shouldSendConnectorDeleteEvent() throws ProducerShutdownException, InvalidProtocolBufferException {
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
  public void shouldSendServiceCreateEvent() throws ProducerShutdownException, InvalidProtocolBufferException {
    CVConfig cvConfig = createCVConfig();

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(cvConfig.getServiceIdentifier(),
        cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier());

    String accountIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String projectIdentifier = generateUuid();
    String identifier = generateUuid();

    IdentifierRefProtoDTO configReference = IdentifierRefProtoDTO.newBuilder()
                                                .setAccountIdentifier(StringValue.of(accountIdentifier))
                                                .setOrgIdentifier(StringValue.of(orgIdentifier))
                                                .setProjectIdentifier(StringValue.of(projectIdentifier))
                                                .setIdentifier(StringValue.of(identifier))
                                                .build();

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
  public void shouldSendServiceDeleteEvent() throws ProducerShutdownException, InvalidProtocolBufferException {
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
  public void shouldSendEnvironmentCreateEvent() throws ProducerShutdownException, InvalidProtocolBufferException {
    CVConfig cvConfig = createCVConfig();

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(cvConfig.getEnvIdentifier(),
        cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier());

    String accountIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String projectIdentifier = generateUuid();
    String identifier = generateUuid();

    IdentifierRefProtoDTO configReference = IdentifierRefProtoDTO.newBuilder()
                                                .setAccountIdentifier(StringValue.of(accountIdentifier))
                                                .setOrgIdentifier(StringValue.of(orgIdentifier))
                                                .setProjectIdentifier(StringValue.of(projectIdentifier))
                                                .setIdentifier(StringValue.of(identifier))
                                                .build();

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
  public void shouldSendEnvironmentDeleteEvent() throws ProducerShutdownException, InvalidProtocolBufferException {
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
  public void shouldSendVerificationJobEnvironmentCreateEvent()
      throws ProducerShutdownException, InvalidProtocolBufferException {
    String accountId = generateUuid();
    String accountIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String projectIdentifier = generateUuid();
    String identifier = generateUuid();

    VerificationJob verificationJob = createVerificationJobDTOWithoutRuntimeParams().getVerificationJob();
    verificationJob.setAccountId(accountId);

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(verificationJob.getEnvIdentifier(),
        verificationJob.getAccountId(), verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier());

    IdentifierRefProtoDTO verificationJobReference = IdentifierRefProtoDTO.newBuilder()
                                                         .setAccountIdentifier(StringValue.of(accountIdentifier))
                                                         .setOrgIdentifier(StringValue.of(orgIdentifier))
                                                         .setProjectIdentifier(StringValue.of(projectIdentifier))
                                                         .setIdentifier(StringValue.of(identifier))
                                                         .build();

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
  public void shouldSendVerificationJobServiceCreateEvent()
      throws ProducerShutdownException, InvalidProtocolBufferException {
    String accountId = generateUuid();
    String accountIdentifier = generateUuid();
    String orgIdentifier = generateUuid();
    String projectIdentifier = generateUuid();
    String identifier = generateUuid();

    VerificationJob verificationJob = createVerificationJobDTOWithoutRuntimeParams().getVerificationJob();
    verificationJob.setAccountId(accountId);

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(verificationJob.getServiceIdentifier(),
        verificationJob.getAccountId(), verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier());

    IdentifierRefProtoDTO verificationJobReference = IdentifierRefProtoDTO.newBuilder()
                                                         .setAccountIdentifier(StringValue.of(accountIdentifier))
                                                         .setOrgIdentifier(StringValue.of(orgIdentifier))
                                                         .setProjectIdentifier(StringValue.of(projectIdentifier))
                                                         .setIdentifier(StringValue.of(identifier))
                                                         .build();

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
  public void shouldSendVerificationJobEnvironmentDeleteEvent()
      throws ProducerShutdownException, InvalidProtocolBufferException {
    String accountId = generateUuid();

    VerificationJob verificationJob = createVerificationJobDTOWithoutRuntimeParams().getVerificationJob();
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
  public void shouldSendVerificationJobServiceDeleteEvent()
      throws ProducerShutdownException, InvalidProtocolBufferException {
    String accountId = generateUuid();

    VerificationJob verificationJob = createVerificationJobDTOWithoutRuntimeParams().getVerificationJob();
    verificationJob.setAccountId(accountId);

    eventService.sendVerificationJobServiceDeleteEvent(verificationJob);

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
            verificationJob.getServiceIdentifier()));
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
