package io.harness.notification;

import static io.harness.logging.LoggingInitializer.initializeLogging;

import static com.google.common.collect.ImmutableMap.of;

import io.harness.Team;
import io.harness.maintenance.MaintenanceController;
import io.harness.metrics.MetricRegistryModule;
import io.harness.ng.core.CorrelationFilter;
import io.harness.ng.core.exceptionmappers.GenericExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.JerseyViolationExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.WingsExceptionMapperV2;
import io.harness.notification.channeldetails.EmailChannel;
import io.harness.notification.channeldetails.MSTeamChannel;
import io.harness.notification.channeldetails.PagerDutyChannel;
import io.harness.notification.channeldetails.SlackChannel;
import io.harness.notification.module.NotificationClientApplicationModule;
import io.harness.notification.notificationclient.NotificationClientImpl;
import io.harness.queue.QueueListenerController;
import io.harness.remote.CharsetResponseFilter;
import io.harness.remote.NGObjectMapperHelper;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.Collections;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

@Slf4j
public class NotificationClientApplication extends Application<NotificationClientApplicationConfiguration> {
  private static final String APPLICATION_NAME = "Notification API Client Test";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));
    new NotificationClientApplication().run(args);
  }

  private final MetricRegistry metricRegistry = new MetricRegistry();

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<NotificationClientApplicationConfiguration> bootstrap) {
    initializeLogging();
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    configureObjectMapper(bootstrap.getObjectMapper());
  }
  public static void configureObjectMapper(final ObjectMapper mapper) {
    NGObjectMapperHelper.configureNGObjectMapper(mapper);
  }

  @Override
  public void run(NotificationClientApplicationConfiguration appConfig, Environment environment) {
    log.info("Starting Next Gen Application ...");
    Injector injector = Guice.createInjector(
        new NotificationClientApplicationModule(appConfig), new MetricRegistryModule(metricRegistry));
    NotificationClientImpl notificationClient = injector.getInstance(NotificationClientImpl.class);
    notificationClient.sendNotificationAsync(EmailChannel.builder()
                                                 .accountId("kmpySmUISimoRrJL6NL73w")
                                                 .recipients(Collections.singletonList("shawank17198@gmail.com"))
                                                 .team(Team.CD)
                                                 .templateId("email_test")
                                                 .templateData(Collections.emptyMap())
                                                 .userGroupIds(Collections.emptyList())
                                                 .build());

    notificationClient.sendNotificationAsync(
        SlackChannel.builder()
            .accountId("kmpySmUISimoRrJL6NL73w")
            .slackWebHookURLs(Collections.singletonList(
                "https://hooks.slack.com/services/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"))
            .team(Team.CD)
            .templateId("slack_test")
            .templateData(Collections.emptyMap())
            .userGroupIds(Collections.emptyList())
            .build());

    notificationClient.sendNotificationAsync(
        PagerDutyChannel.builder()
            .accountId("kmpySmUISimoRrJL6NL73w")
            .pagerDutyIntegrationKeys(Collections.singletonList("b86fd4f114df45808c94b4bce41195da"))
            .team(Team.CD)
            .templateId("pd_test")
            .templateData(Collections.emptyMap())
            .userGroupIds(Collections.emptyList())
            .build());

    notificationClient.sendNotificationAsync(
        MSTeamChannel.builder()
            .accountId("kmpySmUISimoRrJL6NL73w")
            .msTeamKeys(Collections.singletonList(
                "https://outlook.office.com/webhook/54207803-1d3d-44e4-8389-b419334395f2@b229b2bb-5f33-4d22-bce0-730f6474e906/IncomingWebhook/edc3516331d34b2ab54fa906004adbd0/61ab09e6-9850-44f1-b408-3639589f22bf"))
            .team(Team.CD)
            .templateId("msteams_test")
            .templateData(Collections.emptyMap())
            .userGroupIds(Collections.emptyList())
            .build());

    //    notificationClient.testNotificationChannel(EmailSettingDTO.builder()
    //                                                   .accountId("kmpySmUISimoRrJL6NL73w")
    //                                                   .recipient("shawank17198@gmail.com")
    //                                                   .subject("test-subject")
    //                                                   .body("test-body")
    //                                                   .build());
    //
    //    notificationClient.testNotificationChannel(
    //        SlackSettingDTO.builder()
    //            .accountId("kmpySmUISimoRrJL6NL73w")
    //            .recipient("https://hooks.slack.com/services/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")
    //            .build());
    //    notificationClient.testNotificationChannel(
    //        PagerDutySettingDTO.builder().accountId("kmpySmUISimoRrJL6NL73w").recipient("b86fd4f114df45808c94b4bce41195da").build());
    //    notificationClient.testNotificationChannel(
    //        MSTeamSettingDTO.builder()
    //            .accountId("kmpySmUISimoRrJL6NL73w")
    //            .recipient(
    //                "https://outlook.office.com/webhook/54207803-1d3d-44e4-8389-b419334395f2@b229b2bb-5f33-4d22-bce0-730f6474e906/IncomingWebhook/edc3516331d34b2ab54fa906004adbd0/61ab09e6-9850-44f1-b408-3639589f22bf")
    //            .build());
  }

  private void registerJerseyFeatures(Environment environment) {
    environment.jersey().register(MultiPartFeature.class);
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(QueueListenerController.class));
  }

  private void registerCorsFilter(NotificationClientApplicationConfiguration appConfig, Environment environment) {
    FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    String allowedOrigins = String.join(",", appConfig.getAllowedOrigins());
    cors.setInitParameters(of("allowedOrigins", allowedOrigins, "allowedHeaders",
        "X-Requested-With,Content-Type,Accept,Origin,Authorization,X-api-key", "allowedMethods",
        "OPTIONS,GET,PUT,POST,DELETE,HEAD", "preflightMaxAge", "86400"));
    cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
  }

  private void registerJerseyProviders(Environment environment) {
    environment.jersey().register(JerseyViolationExceptionMapperV2.class);
    environment.jersey().register(WingsExceptionMapperV2.class);
    environment.jersey().register(GenericExceptionMapperV2.class);
  }

  private void registerCharsetResponseFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CharsetResponseFilter.class));
  }

  private void registerCorrelationFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CorrelationFilter.class));
  }
}
