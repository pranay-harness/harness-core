/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.openshift;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.sm.ExecutionContext;
import software.wings.utils.ApplicationManifestUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class OpenShiftManagerService {
  @Inject private ApplicationManifestUtils applicationManifestUtils;

  public boolean isOpenShiftManifestConfig(@Nonnull ExecutionContext context) {
    ApplicationManifest applicationManifestForService =
        applicationManifestUtils.getApplicationManifestForService(context);

    if (applicationManifestForService == null) {
      throw new InvalidRequestException("Manifest Config at Service can't be  empty", WingsException.USER);
    }

    return applicationManifestForService.getStoreType() == StoreType.OC_TEMPLATES
        || applicationManifestForService.getStoreType() == StoreType.CUSTOM_OPENSHIFT_TEMPLATE;
  }
}
