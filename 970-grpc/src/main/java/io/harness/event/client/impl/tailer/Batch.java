/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
