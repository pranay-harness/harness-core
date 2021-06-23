package io.harness.pms.event.featureflag;

import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.helpers.PmsFeatureFlagHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class PipelineServiceFeatureFlagListener implements MessageListener {
  @Inject private PmsFeatureFlagHelper pmsFeatureFlagService;

  @Override
  public boolean handleMessage(Message message) {
    try {
      if (message != null && message.hasMessage()) {
        FeatureFlagChangeDTO featureFlagChangeDTO;
        try {
          featureFlagChangeDTO = FeatureFlagChangeDTO.parseFrom(message.getMessage().getData());
        } catch (InvalidProtocolBufferException e) {
          throw new InvalidRequestException(
              String.format("Exception in unpacking FeatureFlagChangeDTO for key %s", message.getId()), e);
        }
        pmsFeatureFlagService.updateCache(featureFlagChangeDTO.getAccountId(), featureFlagChangeDTO.getEnable(),
            featureFlagChangeDTO.getFeatureName());
      }
    } catch (Exception ex) {
      log.error("Error updating feature flag cache for pipeline-service", ex);
    }
    return true;
  }
}
