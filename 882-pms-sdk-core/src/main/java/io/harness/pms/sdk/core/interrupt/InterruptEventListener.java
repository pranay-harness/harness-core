package io.harness.pms.sdk.core.interrupt;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.contracts.interrupts.InterruptEvent.Builder;
import io.harness.pms.execution.utils.InterruptEventUtils;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
@Deprecated
public class InterruptEventListener extends QueueListener<io.harness.pms.interrupts.InterruptEvent> {
  @Inject InterruptEventHandler interruptEventHandler;

  @Inject
  public InterruptEventListener(QueueConsumer<io.harness.pms.interrupts.InterruptEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(io.harness.pms.interrupts.InterruptEvent event) {
    Builder interruptEventBuilder = io.harness.pms.contracts.interrupts.InterruptEvent.newBuilder()
                                        .setType(event.getInterruptType())
                                        .setInterruptUuid(event.getInterruptUuid())
                                        .setNotifyId(event.getNotifyId())
                                        .putAllMetadata(CollectionUtils.emptyIfNull(event.getMetadata()));
    interruptEventHandler.handleEvent(populateResponse(event.getNodeExecution(), interruptEventBuilder));
  }

  private InterruptEvent populateResponse(NodeExecutionProto nodeExecutionProto, Builder builder) {
    int responseCount = nodeExecutionProto.getExecutableResponsesCount();
    if (responseCount <= 0) {
      return null;
    }
    ExecutableResponse executableResponse = nodeExecutionProto.getExecutableResponses(responseCount - 1);
    return InterruptEventUtils.buildInterruptEvent(builder, executableResponse);
  }
}
