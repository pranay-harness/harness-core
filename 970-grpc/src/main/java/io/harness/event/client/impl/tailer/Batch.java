/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.client.impl.tailer;

import static com.google.common.base.Preconditions.checkState;

import io.harness.event.PublishMessage;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class Batch {
  private final int thresholdBytes;
  private final int thresholdCount;

  @Getter private final List<PublishMessage> messages = new ArrayList<>();
  private int byteSize;

  void add(PublishMessage message) {
    checkState(!isFull(), "Adding to a full batch");
    messages.add(message);
    byteSize += message.getSerializedSize();
  }

  public boolean isEmpty() {
    return messages.isEmpty();
  }

  public int size() {
    return messages.size();
  }

  public boolean isFull() {
    return size() >= thresholdCount || byteSize >= thresholdBytes;
  }
}
