/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.grpc.utils;

import static io.harness.configuration.DeployMode.DEPLOY_MODE;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

/**
 * Utility class for the methods to extract grpc target & authority from manager url. Used to provide values while
 * processing delegate config templates.
 */
@UtilityClass
public class DelegateGrpcConfigExtractor {
  private static String MANAGER_GRPC_PORT = "9879";
  public static String extractTarget(String managerUrl) {
    try {
      if ("KUBERNETES_ONPREM".equals(System.getenv().get(DEPLOY_MODE))) {
        return new URI(managerUrl).getHost() + ":" + MANAGER_GRPC_PORT;
      }
      return new URI(managerUrl).getAuthority();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid manager url " + managerUrl, e);
    }
  }

  public static String extractAuthority(String managerUrl, String svc) {
    try {
      URI uri = new URI(managerUrl);
      if ("KUBERNETES_ONPREM".equals(System.getenv().get(DEPLOY_MODE))) {
        if ("https".equals(extractScheme(managerUrl))) {
          return uri.getAuthority();
        } else {
          return "default-authority.harness.io";
        }
      }
      String path = uri.getPath();
      String[] parts = path.split("/");
      String prefix = null;
      if (parts.length > 1) {
        prefix = parts[1];
      }
      return Stream.of(prefix, svc, "grpc", uri.getAuthority())
          .filter(Objects::nonNull)
          .collect(Collectors.joining("-"));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid manager url " + managerUrl, e);
    }
  }

  public static String extractScheme(String managerUrl) {
    try {
      return new URI(managerUrl).getScheme();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid manager url " + managerUrl, e);
    }
  }
}
