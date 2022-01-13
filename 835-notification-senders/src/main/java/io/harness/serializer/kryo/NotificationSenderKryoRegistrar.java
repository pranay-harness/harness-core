/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.MailTaskParams;
import io.harness.delegate.beans.MicrosoftTeamsTaskParams;
import io.harness.delegate.beans.NotificationTaskResponse;
import io.harness.delegate.beans.PagerDutyTaskParams;
import io.harness.delegate.beans.SlackTaskParams;
import io.harness.notification.beans.NotificationProcessingResponse;
import io.harness.notification.remote.SmtpConfigResponse;
import io.harness.serializer.KryoRegistrar;

import software.wings.beans.NotificationChannelType;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.notification.SlackNotificationSetting;

import com.esotericsoftware.kryo.Kryo;
import com.github.dikhan.pagerduty.client.events.domain.LinkContext;
import com.github.dikhan.pagerduty.client.events.domain.Payload;
import com.github.dikhan.pagerduty.client.events.domain.Severity;

@OwnedBy(PL)
public class NotificationSenderKryoRegistrar implements KryoRegistrar {
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
    kryo.register(SmtpConfigResponse.class, 55219);
    kryo.register(NotificationSettings.class, 5626);
    kryo.register(NotificationChannelType.class, 7115);
    kryo.register(SlackNotificationSetting.class, 7119);
  }
}
