package io.harness.pms.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.pipeline.observer.PipelineActionObserver;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;

@Singleton
@OwnedBy(PIPELINE)
public class PipelineEntityCrudObserver implements PipelineActionObserver {
  @Inject @Named(EventsFrameworkConstants.ENTITY_CRUD) private Producer eventProducer;

  @Override
  public void onDelete(PipelineEntity pipelineEntity) {
    EntityChangeDTO.Builder pipelineEntityChangeDTOBuilder =
        EntityChangeDTO.newBuilder()
            .setAccountIdentifier(StringValue.of(pipelineEntity.getAccountId()))
            .setOrgIdentifier(StringValue.of(pipelineEntity.getOrgIdentifier()))
            .setProjectIdentifier(StringValue.of(pipelineEntity.getProjectIdentifier()))
            .setIdentifier(StringValue.of(pipelineEntity.getIdentifier()));

    try {
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", pipelineEntity.getAccountId(),
                  EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.PIPELINE_ENTITY,
                  EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.DELETE_ACTION))
              .setData(pipelineEntityChangeDTOBuilder.build().toByteString())
              .build());
    } catch (ProducerShutdownException ex) {
      throw new InvalidRequestException("Redis Producer shutdown unexpectedly", ex);
    }
  }
}
