package io.harness.batch.processing.listener;

import io.harness.event.grpc.PublishedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EcsStepSkipListener implements SkipListener<PublishedMessage, PublishedMessage> {
  @Override
  public void onSkipInRead(Throwable throwable) {
    logger.error("EcsStepSkipListener onSkipInRead");
  }

  @Override
  public void onSkipInWrite(PublishedMessage publishedMessage, Throwable throwable) {
    logger.error("EcsStepSkipListener onSkipInWrite {} ", publishedMessage);
  }

  @Override
  public void onSkipInProcess(PublishedMessage publishedMessage, Throwable throwable) {
    logger.error("EcsStepSkipListener onSkipInProcess {}", publishedMessage);
  }
}
