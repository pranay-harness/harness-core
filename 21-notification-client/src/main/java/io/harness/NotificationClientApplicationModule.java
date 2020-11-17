package io.harness;

import com.google.inject.AbstractModule;

public class NotificationClientApplicationModule extends AbstractModule {
  private final NotificationClientApplicationConfiguration appConfig;

  public NotificationClientApplicationModule(NotificationClientApplicationConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  protected void configure() {
    install(new NotificationClientModule(this.appConfig.getNotificationClientConfiguration()));
  }
}
