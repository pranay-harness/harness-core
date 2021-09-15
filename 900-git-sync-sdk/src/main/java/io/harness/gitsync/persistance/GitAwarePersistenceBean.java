/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Injector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

@EnableGuiceModules
@Configuration
@OwnedBy(DX)
public class GitAwarePersistenceBean {
  @Autowired private Injector injector;

  @Bean
  public GitAwarePersistence myModule() {
    return injector.getInstance(GitAwarePersistence.class);
  }
}
