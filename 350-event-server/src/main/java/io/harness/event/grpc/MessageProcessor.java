package io.harness.event.grpc;

public interface MessageProcessor { void process(PublishedMessage publishedMessage); }
