package io.harness.batch.processing.listener;

import io.harness.ccm.commons.entities.events.PublishedMessage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoggingStepSkipListener implements SkipListener<PublishedMessage, PublishedMessage> {
  @Override
  public void onSkipInRead(Throwable throwable) {
    log.error("Skip in read", throwable);
  }

  @Override
  public void onSkipInWrite(PublishedMessage publishedMessage, Throwable throwable) {
    log.error("Skip in write for message {} ", publishedMessage.getUuid(), throwable);
  }

  @Override
  public void onSkipInProcess(PublishedMessage publishedMessage, Throwable throwable) {
    log.error("Skip in process for message {}", publishedMessage.getUuid(), throwable);
  }
}
