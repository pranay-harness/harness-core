package io.harness.telemetry.segment.remote;

import io.harness.network.Http;
import io.harness.telemetry.TelemetryConfiguration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
@Slf4j
public class RemoteSegmentClientFactory implements Provider<RemoteSegmentClient> {
  private TelemetryConfiguration telemetryConfiguration;

  @Inject
  public RemoteSegmentClientFactory(TelemetryConfiguration telemetryConfiguration) {
    this.telemetryConfiguration = telemetryConfiguration;
  }

  @Override
  public RemoteSegmentClient get() {
    final Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(telemetryConfiguration.getUrl())
            .addConverterFactory(JacksonConverterFactory.create())
            .client(telemetryConfiguration.isCertValidationRequired()
                    ? Http.getSafeOkHttpClientBuilder(telemetryConfiguration.getUrl(), 15, 20).build()
                    : Http.getUnsafeOkHttpClient(telemetryConfiguration.getUrl(), 15, 20))
            .build();
    return retrofit.create(RemoteSegmentClient.class);
  }
}
