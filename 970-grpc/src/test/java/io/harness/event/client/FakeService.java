/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.client;

import io.harness.event.EventPublisherGrpc;
import io.harness.event.PublishMessage;
import io.harness.event.PublishRequest;
import io.harness.event.PublishResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FakeService extends EventPublisherGrpc.EventPublisherImplBase {
  private static final int FAIL_PERCENT = 10;
  private final AtomicInteger messageCounter = new AtomicInteger();
  private final List<PublishMessage> receivedMessages = Collections.synchronizedList(new ArrayList<>());
  private final AtomicBoolean failNext = new AtomicBoolean(false);

  private volatile boolean errorProne;
  private volatile boolean recordMessages;

  public int getMessageCount() {
    return messageCounter.get();
  }

  public void setErrorProne(boolean errorProne) {
    this.errorProne = errorProne;
  }

  public void failNext() {
    this.failNext.set(true);
  }

  public void setRecordMessages(boolean recordMessages) {
    this.recordMessages = recordMessages;
  }

  public List<PublishMessage> getReceivedMessages() {
    return Collections.unmodifiableList(receivedMessages);
  }

  private boolean shouldFailCall() {
    return failNext.getAndSet(false) || errorProne && ThreadLocalRandom.current().nextInt(100) < FAIL_PERCENT;
  }

  @Override
  public void publish(PublishRequest request, StreamObserver<PublishResponse> responseObserver) {
    if (shouldFailCall()) {
      responseObserver.onError(Status.UNKNOWN.withDescription("Random Error").asException());
    } else {
      messageCounter.addAndGet(request.getMessagesCount());
      if (recordMessages) {
        receivedMessages.addAll(request.getMessagesList());
      }
      responseObserver.onNext(PublishResponse.newBuilder().build());
      responseObserver.onCompleted();
    }
  }
}
