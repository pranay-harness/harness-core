package io.harness.core.userchangestream;

import io.harness.mongo.MongoConfig;
import io.harness.mongo.changestreams.ChangeEventFactory;
import io.harness.mongo.changestreams.ChangeStreamModule;
import io.harness.mongo.changestreams.ChangeTracker;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Named;

public class UserMembershipChangeStreamModule extends AbstractModule {
  private static volatile UserMembershipChangeStreamModule instance;

  public static UserMembershipChangeStreamModule getInstance() {
    if (instance == null) {
      instance = new UserMembershipChangeStreamModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(ChangeStreamModule.getInstance());
  }

  @Provides
  @Named("UserMembership")
  public ChangeTracker getChangeTracker(Injector injector) {
    MongoConfig mongoConfig = injector.getInstance(MongoConfig.class);
    ChangeEventFactory changeEventFactory = injector.getInstance(ChangeEventFactory.class);
    return new ChangeTracker(mongoConfig, changeEventFactory, null);
  }
}
