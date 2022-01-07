/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response.Status;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

/**
 * Created by anubhaw on 5/11/16.
 */
@Slf4j
public class IntegrationTestUtils {
  private static final SecureRandom random = new SecureRandom();

  /**
   * Creates the hosts file.
   *
   * @param file     the file
   * @param numHosts the num hosts
   * @return the file
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static File createHostsFile(File file, int numHosts) throws IOException {
    try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
      out.write("HOST\n"); // Header
      for (int idx = 1; idx <= numHosts; idx++) {
        out.write(format("host%s.app.com\n", idx));
      }
    }
    return file;
  }

  /**
   * Random int.
   *
   * @param low  the low
   * @param high the high
   * @return the int
   */
  public static int randomInt(int low, int high) {
    return random.nextInt(high - low) + low;
  }

  /**
   * Random int.
   *
   * @param high the high
   * @return the int
   */
  public static int randomInt(int high) {
    return randomInt(0, high);
  }

  /**
   * Random int.
   *
   * @return the int
   */
  public static int randomInt() {
    return randomInt(0, MAX_VALUE);
  }

  public static boolean isManagerRunning(Client client) throws URISyntaxException {
    try {
      String url = buildAbsoluteUrl("/api/version", new HashMap<>());
      WebTarget target = client.target(url);
      int status = target.request().get().getStatus();
      return status == Status.OK.getStatusCode();
    } catch (ProcessingException e) {
      if (e.getCause() instanceof ConnectException) {
        return false;
      }
    }

    return false;
  }

  /**
   * @param path example: /api/dash-stats
   * @param params
   * @return fully formed URL string
   * @throws URISyntaxException
   */
  public static String buildAbsoluteUrl(String path, Map<String, String> params) throws URISyntaxException {
    URIBuilder uriBuilder = new URIBuilder();
    String scheme = StringUtils.isBlank(System.getenv().get("BASE_HTTP")) ? "https" : "http";
    uriBuilder.setScheme(scheme);
    uriBuilder.setHost("localhost");
    uriBuilder.setPort(9090);
    uriBuilder.setPath(path);
    if (params != null) {
      params.forEach((name, value) -> uriBuilder.addParameter(name, value.toString()));
    }
    return uriBuilder.build().toString();
  }
}
