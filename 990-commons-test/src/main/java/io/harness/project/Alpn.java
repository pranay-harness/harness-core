/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.project;

import java.io.File;

public class Alpn {
  public static String ALPN_FILENAME = "alpn-boot-8.1.13.v20181017.jar";
  public static String MVN_REPO_PATH = "/.m2/repository";
  public static String ARTIFACT_PATH = "/org/mortbay/jetty/alpn/alpn-boot/8.1.13.v20181017";

  public static String location() {
    String alpn = "/harness"
        + "/" + ALPN_FILENAME;
    if (new File(alpn).exists()) {
      return alpn;
    }

    alpn = System.getProperty("user.dir") + "/" + ALPN_FILENAME;
    if (new File(alpn).exists()) {
      return alpn;
    }

    alpn = System.getProperty("user.home") + MVN_REPO_PATH + ARTIFACT_PATH + "/" + ALPN_FILENAME;
    if (new File(alpn).exists()) {
      return alpn;
    }

    alpn = "/home/jenkins/maven-repositories/0" + ARTIFACT_PATH + "/" + ALPN_FILENAME;
    if (new File(alpn).exists()) {
      return alpn;
    }

    throw new RuntimeException("Did not find alpn");
  }
}
