package io.harness.marketplace.gcp.servicecontrol;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.servicecontrol.v1.ServiceControl;
import com.google.api.services.servicecontrol.v1.ServiceControlScopes;
import com.google.inject.Singleton;

import io.harness.marketplace.gcp.GcpMarketPlaceConstants;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

@Slf4j
@Singleton
public class ServiceControlAPIClientBuilder {
  private ServiceControl client;

  public Optional<ServiceControl> getInstance() {
    if (null != this.client) {
      return Optional.of(client);
    }

    HttpTransport httpTransport = Utils.getDefaultTransport();
    JsonFactory jsonFactory = Utils.getDefaultJsonFactory();

    Path path = Paths.get(GcpMarketPlaceConstants.SERVICE_ACCOUNT_INTEGRATION_PATH);

    if (!Files.exists(path)) {
      logger.error(
          "GCP_MKT_PLACE credentials file does NOT exist. Marketplace Approval requests will fail. Path: {}", path);
      return Optional.empty();
    }

    InputStream credentialStream;
    try {
      credentialStream = Files.newInputStream(path, StandardOpenOption.READ);

    } catch (IOException e) {
      logger.error("GCP_MKT_PLACE exception reading credentials file. Path: " + path, e);
      return Optional.empty();
    }

    GoogleCredential credential;
    try {
      credential = GoogleCredential.fromStream(credentialStream);
    } catch (IOException e) {
      logger.error("GCP_MKT_PLACE exception creating google credentials from credential stream. Path: " + path, e);
      return Optional.empty();
    }

    if (credential.createScopedRequired()) {
      credential = credential.createScoped(ServiceControlScopes.all());
    }

    ServiceControl serviceControl = new ServiceControl.Builder(httpTransport, jsonFactory, credential)
                                        .setApplicationName(GcpMarketPlaceConstants.HARNESS_GCP_APPLICATION)
                                        .setRootUrl(GcpMarketPlaceConstants.SERVICE_CONTROL_API_END_POINT)
                                        .build();

    client = serviceControl;
    return Optional.of(client);
  }
}
