package io.harness.gitsync;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class GitSyncSdkInitHelper {
  public static void initGitSyncSdk(Injector injector, GitSyncSdkConfiguration config) {
    String serviceName = config.getMicroservice().name();
    log.info("Initializing GMS SDK for service: {}", serviceName);
    ServiceManager serviceManager =
        injector.getInstance(Key.get(ServiceManager.class, Names.named("gitsync-sdk-service-manager"))).startAsync();
    serviceManager.awaitHealthy();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));
  }
}
