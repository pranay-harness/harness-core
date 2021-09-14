/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.controller;

import static io.harness.beans.PrimaryVersion.Builder.aConfiguration;
import static io.harness.beans.PrimaryVersion.MATCH_ALL_VERSION;

import io.harness.beans.PrimaryVersion;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.version.VersionInfoManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Singleton
@Slf4j
public class PrimaryVersionController implements QueueController, Runnable {
  @Inject private HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;

  private final AtomicBoolean primary = new AtomicBoolean(true);
  private final AtomicReference<String> primaryVersion = new AtomicReference<>(MATCH_ALL_VERSION);

  public PrimaryVersionController() {}

  @Override
  public boolean isPrimary() {
    return primary.get();
  }

  @Override
  public boolean isNotPrimary() {
    return !primary.get();
  }

  public String getPrimaryVersion() {
    return primaryVersion.get();
  }

  public void run() {
    try {
      PrimaryVersion primaryVersion = persistence.createQuery(PrimaryVersion.class).get();
      if (primaryVersion == null) {
        primaryVersion = aConfiguration().withPrimaryVersion(MATCH_ALL_VERSION).build();
        persistence.save(primaryVersion);
      }

      if (!StringUtils.equals(this.primaryVersion.get(), primaryVersion.getPrimaryVersion())) {
        log.info(
            "Changing primary version from {} to {}", this.primaryVersion.get(), primaryVersion.getPrimaryVersion());
        this.primaryVersion.set(primaryVersion.getPrimaryVersion());
      }

      boolean isPrimary = StringUtils.equals(MATCH_ALL_VERSION, primaryVersion.getPrimaryVersion())
          || StringUtils.equals(versionInfoManager.getVersionInfo().getVersion(), primaryVersion.getPrimaryVersion());
      primary.set(isPrimary);
    } catch (Exception e) {
      log.error("Exception occurred while running primary version change scheduler", e);
    }
  }
}
