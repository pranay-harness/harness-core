/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.utils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.manifest.CustomSourceConfig;

import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.service.intfc.ApplicationManifestService;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Singleton
public class ApplicationManifestHelper {
  @Inject private ApplicationManifestService applicationManifestService;

  public void createCustomManifest(
      Service service, Environment env, String scriptResource, String path, AppManifestKind kind) {
    createCustomManifest(env.getAppId(), service.getUuid(), env.getUuid(), scriptResource, path, kind);
  }

  public void createCustomManifest(
      String appId, String serviceId, String envId, String scriptResource, String path, AppManifestKind kind) {
    String customManifestScript = "";
    if (isNotEmpty(scriptResource)) {
      customManifestScript = getResourceContent(scriptResource);
    }

    ApplicationManifest applicationManifest =
        ApplicationManifest.builder()
            .storeType(StoreType.CUSTOM)
            .kind(kind)
            .customSourceConfig(CustomSourceConfig.builder().path(path).script(customManifestScript).build())
            .envId(envId)
            .serviceId(serviceId)
            .accountId(serviceId)
            .build();
    applicationManifest.setAppId(appId);
    create(serviceId, envId, applicationManifest);
  }

  public ApplicationManifest create(String serviceId, String envId, ApplicationManifest applicationManifest) {
    ApplicationManifest existing = applicationManifestService.getAppManifest(
        applicationManifest.getAppId(), envId, serviceId, applicationManifest.getKind());
    if (existing != null) {
      applicationManifest.setUuid(existing.getUuid());
      return applicationManifestService.update(applicationManifest);
    } else {
      return applicationManifestService.create(applicationManifest);
    }
  }

  public void cleanup(Service service, Environment env, AppManifestKind kind) {
    String serviceId = service != null ? service.getUuid() : null;
    String envId = env != null ? env.getUuid() : null;
    String appId = service != null ? service.getAppId() : (env != null ? env.getUuid() : null);
    ApplicationManifest existing = applicationManifestService.getAppManifest(appId, envId, serviceId, kind);
    if (existing != null) {
      applicationManifestService.deleteAppManifest(appId, existing.getUuid());
    }
  }

  private String getResourceContent(String resourcePath) {
    try {
      URL url = ApplicationManifestHelper.class.getClassLoader().getResource(resourcePath);
      return url != null ? Resources.toString(url, StandardCharsets.UTF_8) : "";
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
