package io.harness.feature.services;

import com.google.inject.Injector;

public interface FeaturesManagementJob {
  void run(Injector injector);
}
