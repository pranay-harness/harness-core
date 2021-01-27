package io.harness.pms.pipeline;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.harness.PipelineServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateV2DTO;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PipelineSetupUsageHelperTest extends PipelineServiceTestBase {
  @Mock private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;
  @Mock private Producer eventProducer;

  @InjectMocks private PipelineSetupUsageHelper pipelineSetupUsageHelper;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    when(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO("accountId", null, null, null))
        .thenReturn(IdentifierRefProtoDTO.newBuilder().build());
  }

  @After
  public void verifyMocks() {
    verifyNoMoreInteractions(eventProducer);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testPublishSetupUsageEvent() throws ProducerShutdownException {
    List<EntityDetailProtoDTO> referredEntities = new ArrayList<>();
    EntityDetailProtoDTO connectorManagerDetails = EntityDetailProtoDTO.newBuilder()
                                                       .setIdentifierRef(IdentifierRefProtoDTO.newBuilder().build())
                                                       .setType(EntityTypeProtoEnum.SECRETS)
                                                       .build();
    EntityDetailProtoDTO secretManagerDetails = EntityDetailProtoDTO.newBuilder()
                                                    .setIdentifierRef(IdentifierRefProtoDTO.newBuilder().build())
                                                    .setType(EntityTypeProtoEnum.CONNECTORS)
                                                    .build();
    referredEntities.add(secretManagerDetails);
    referredEntities.add(connectorManagerDetails);

    PipelineEntity pipelineEntity = PipelineEntity.builder().name("test").accountId("accountId").build();
    EntityDetailProtoDTO pipelineDetails =
        EntityDetailProtoDTO.newBuilder()
            .setIdentifierRef(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(pipelineEntity.getAccountId(),
                pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(),
                pipelineEntity.getIdentifier()))
            .setType(EntityTypeProtoEnum.PIPELINES)
            .setName(pipelineEntity.getName())
            .build();
    EntitySetupUsageCreateV2DTO secretEntityReferenceDTO =
        EntitySetupUsageCreateV2DTO.newBuilder()
            .setAccountIdentifier(pipelineEntity.getAccountId())
            .setReferredByEntity(pipelineDetails)
            .addAllReferredEntities(Lists.newArrayList(connectorManagerDetails))
            .setDeleteOldReferredByRecords(true)
            .build();
    EntitySetupUsageCreateV2DTO connectorEntityReferenceDTO1 =
        EntitySetupUsageCreateV2DTO.newBuilder()
            .setAccountIdentifier(pipelineEntity.getAccountId())
            .setReferredByEntity(pipelineDetails)
            .addAllReferredEntities(Lists.newArrayList(secretManagerDetails))
            .setDeleteOldReferredByRecords(true)
            .build();

    pipelineSetupUsageHelper.publishSetupUsageEvent(pipelineEntity, referredEntities);

    verify(eventProducer)
        .send(Message.newBuilder()
                  .putAllMetadata(ImmutableMap.of("accountId", pipelineEntity.getAccountId(),
                      EventsFrameworkMetadataConstants.REFERRED_ENTITY_TYPE, EntityTypeProtoEnum.SECRETS.name(),
                      EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.FLUSH_CREATE_ACTION))
                  .setData(secretEntityReferenceDTO.toByteString())
                  .build());
    verify(eventProducer)
        .send(Message.newBuilder()
                  .putAllMetadata(ImmutableMap.of("accountId", pipelineEntity.getAccountId(),
                      EventsFrameworkMetadataConstants.REFERRED_ENTITY_TYPE, EntityTypeProtoEnum.CONNECTORS.name(),
                      EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.FLUSH_CREATE_ACTION))
                  .setData(connectorEntityReferenceDTO1.toByteString())
                  .build());
  }
}