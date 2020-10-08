package io.harness.perpetualtask.datacollection;

import com.google.inject.Singleton;

import io.harness.perpetualtask.k8s.watch.K8sWatchServiceDelegate.WatcherGroup;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Data
public class K8ActivityCollectionWatches {
  private final Map<String, WatcherGroup> watchMap;

  public K8ActivityCollectionWatches() {
    this.watchMap = new ConcurrentHashMap<>();
  }
}
