package io.harness.mongo.queue;

import static io.harness.manage.GlobalContextManager.obtainGlobalContext;
import static io.harness.queue.Queue.VersionType.VERSIONED;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import io.harness.queue.Queuable;
import io.harness.queue.QueuePublisher;
import io.harness.version.VersionInfoManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class MongoQueuePublisher<T extends Queuable> implements QueuePublisher<T> {
  @Getter private final String name;
  private final VersionType versionType;

  @Inject private HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;

  public MongoQueuePublisher(String name, VersionType versionType) {
    this.name = name;
    this.versionType = versionType;
  }

  @Override
  public void send(final T payload) {
    Objects.requireNonNull(payload);
    payload.setGlobalContext(obtainGlobalContext());
    payload.setVersion(versionType == VERSIONED ? versionInfoManager.getVersionInfo().getVersion() : null);
    persistence.insertIgnoringDuplicateKeys(payload);
  }
}
