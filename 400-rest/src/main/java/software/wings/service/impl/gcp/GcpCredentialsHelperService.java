package software.wings.service.impl.gcp;

import software.wings.beans.GcpConfig;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.container.ContainerScopes;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Singleton
@Slf4j
public class GcpCredentialsHelperService {
  @Inject private GcpHttpTransportHelperService gcpHttpTransportHelperService;

  public GoogleCredential getGoogleCredentialWithDefaultHttpTransport(GcpConfig gcpConfig) throws IOException {
    return appendScopesIfRequired(GoogleCredential.fromStream(
        IOUtils.toInputStream(String.valueOf(gcpConfig.getServiceAccountKeyFileContent()), Charset.defaultCharset())));
  }

  public GoogleCredential getGoogleCredentialWithProxyConfiguredHttpTransport(GcpConfig gcpConfig) throws IOException {
    HttpTransport httpTransport = gcpHttpTransportHelperService.getProxyConfiguredHttpTransport();
    return appendScopesIfRequired(GoogleCredential.fromStream(
        IOUtils.toInputStream(String.valueOf(gcpConfig.getServiceAccountKeyFileContent()), Charset.defaultCharset()),
        httpTransport, JacksonFactory.getDefaultInstance()));
  }

  private GoogleCredential appendScopesIfRequired(GoogleCredential googleCredential) {
    if (googleCredential.createScopedRequired()) {
      return googleCredential.createScoped(Collections.singletonList(ContainerScopes.CLOUD_PLATFORM));
    }
    return googleCredential;
  }
}
