package io.harness.pms.downloadlogs.helpers;

import io.harness.yaml.core.timeout.Timeout;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class DownloadLogsHelper {
  public String generateDownloadLink(String accountId, String logKey) {
    // Here we will get download link from log service
    String downloadLink = "MockLink";
    return downloadLink;
  }
}
