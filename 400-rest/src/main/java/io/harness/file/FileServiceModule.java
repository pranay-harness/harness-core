/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.file;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.service.impl.FileServiceImpl.FILE_SERVICE_DATA_STORAGE_MODE;
import static software.wings.service.impl.GoogleCloudFileServiceImpl.FILE_SERVICE_CLUSTER_NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.file.dao.GcsHarnessFileMetadataDao;
import io.harness.file.dao.WingsGcsFileMetadataDaoImpl;
import io.harness.persistence.HPersistence;

import software.wings.DataStorageMode;
import software.wings.service.impl.FileServiceImpl;
import software.wings.service.intfc.FileService;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@OwnedBy(PL)
public class FileServiceModule extends AbstractModule {
  private final DataStorageMode dataStorageMode;
  private final String clusterName;

  public FileServiceModule(DataStorageMode dataStorageMode, String clusterName) {
    this.dataStorageMode = dataStorageMode;
    this.clusterName = clusterName;
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
    bind(GcsHarnessFileMetadataDao.class).to(WingsGcsFileMetadataDaoImpl.class);
    bind(FileService.class).to(FileServiceImpl.class);
  }

  @Provides
  @Singleton
  @Named(FILE_SERVICE_CLUSTER_NAME)
  protected String getClusterName() {
    return this.clusterName;
  }

  @Provides
  @Singleton
  @Named(FILE_SERVICE_DATA_STORAGE_MODE)
  protected DataStorageMode getDataStorageMode() {
    return this.dataStorageMode;
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
