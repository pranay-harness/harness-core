/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.scheduler;

import software.wings.app.MainConfiguration;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class BackgroundJobScheduler extends JobScheduler {
  @Inject
  public BackgroundJobScheduler(Injector injector, MainConfiguration configuration) {
    super(injector, configuration.getBackgroundSchedulerConfig(), configuration.getMongoConnectionFactory().getUri());
  }
}
