package io.harness.event.client;

import com.squareup.tape2.ObjectQueue.Converter;
import io.harness.event.PublishMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Specify the serialization mechanism of PublishMessage for persisting to queue file.
 */
@Slf4j
@ParametersAreNonnullByDefault
public class PublishMessageConverter implements Converter<PublishMessage> {
  @Override
  public PublishMessage from(byte[] bytes) throws IOException {
    return PublishMessage.parseFrom(bytes);
  }

  @Override
  public void toStream(PublishMessage message, OutputStream sink) throws IOException {
    message.writeTo(sink);
  }
}
