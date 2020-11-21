package io.harness.perpetualtask.datacollection;

import io.harness.perpetualtask.k8s.watch.K8sWatchServiceDelegate.WatcherGroup;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;

@Singleton
@Data
public class K8ActivityCollectionWatches {
  private final Map<String, WatcherGroup> watchMap;

  public K8ActivityCollectionWatches() {
    this.watchMap = new ConcurrentHashMap<>();
  }
}
