package io.harness.feature.services;

import com.google.inject.Injector;

public interface FeatureLoader {
  void run(Injector injector);
}
