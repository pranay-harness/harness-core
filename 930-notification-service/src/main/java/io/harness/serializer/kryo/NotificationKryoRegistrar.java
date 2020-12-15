package io.harness.serializer.kryo;

import io.harness.delegate.beans.*;
import io.harness.notification.beans.NotificationProcessingResponse;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;
import com.github.dikhan.pagerduty.client.events.domain.LinkContext;
import com.github.dikhan.pagerduty.client.events.domain.Payload;
import com.github.dikhan.pagerduty.client.events.domain.Severity;

public class NotificationKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(SlackTaskParams.class, 55210);
    kryo.register(MailTaskParams.class, 55211);
    kryo.register(PagerDutyTaskParams.class, 55212);
    kryo.register(MicrosoftTeamsTaskParams.class, 55213);
    kryo.register(Payload.class, 55214);
    kryo.register(LinkContext.class, 55215);
    kryo.register(NotificationTaskResponse.class, 55216);
    kryo.register(NotificationProcessingResponse.class, 55217);
    kryo.register(Severity.class, 55218);
  }
}