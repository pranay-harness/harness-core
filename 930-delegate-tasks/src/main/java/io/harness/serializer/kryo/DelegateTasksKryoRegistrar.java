/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskParameters;
import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskResponse;
import io.harness.serializer.KryoRegistrar;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.trigger.WebHookTriggerResponseData;
import software.wings.beans.trigger.WebhookTriggerParameters;
import software.wings.delegatetasks.cv.DataCollectionException;

import com.esotericsoftware.kryo.Kryo;

public class DelegateTasksKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ExecutionLogCallback.class, 5044);
    kryo.register(DataCollectionException.class, 7298);
    kryo.register(BatchCapabilityCheckTaskParameters.class, 8200);
    kryo.register(BatchCapabilityCheckTaskResponse.class, 8201);
    kryo.register(WebhookTriggerParameters.class, 8550);
    kryo.register(WebHookTriggerResponseData.class, 8552);
  }
}
