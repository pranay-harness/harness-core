package io.harness;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.grpc.BindableService;
import io.grpc.services.HealthStatusManager;
import io.harness.grpc.server.GrpcServer;
import io.harness.pms.sdk.creator.PlanCreatorProvider;
import io.harness.pms.sdk.creator.PlanCreatorService;
import io.harness.serializer.KryoSerializer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotNull;

public class PmsSdkModule extends AbstractModule {
  private static PmsSdkModule instance;

  public static PmsSdkModule getInstance() {
    if (instance == null) {
      instance = new PmsSdkModule();
    }
    return instance;
  }

  @Override
  protected void configure() {}

  @Provides
  @Singleton
  public PlanCreatorService planCreatorService(KryoSerializer kryoSerializer, PlanCreatorProvider planCreatorProvider) {
    return new PlanCreatorService(kryoSerializer, planCreatorProvider);
  }
}
