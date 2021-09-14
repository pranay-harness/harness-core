/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

@Slf4j
public class TestMetadataListener extends RunListener {
  private static final String SUREFIRE_REPORTS_FOLDER = "target/surefire-reports";

  private final Map<String, List<TestMetadata>> testMetadataMap;
  private final String outputFolder;

  public TestMetadataListener(String outputFolder) {
    this.outputFolder = outputFolder;
    this.testMetadataMap = new ConcurrentHashMap<>();
  }

  public TestMetadataListener() {
    this(SUREFIRE_REPORTS_FOLDER);
  }

  @Override
  public void testStarted(Description description) throws Exception {
    val className = description.getClassName();
    val methodName = description.getMethodName();
    val testMetadataBuilder = TestMetadata.builder().className(className).methodName(methodName);
    val owner = description.getAnnotation(Owner.class);
    if (owner != null) {
      testMetadataBuilder.developer(owner.developers()[0]);
    }
    TestMetadata testMetadata = testMetadataBuilder.build();
    testMetadataMap.computeIfAbsent(className, k -> new ArrayList<>()).add(testMetadata);
  }

  public static void ignoredOnPurpose(Throwable exception) {
    // We would like to express explicitly that we are ignoring this exception
  }

  @Override
  public void testRunFinished(Result result) throws Exception {
    // With forks on, we occasionally read file while it's partially written resulting in parse exception
    // Retrying is a workaround.
    for (int i = 0; i < 3; i++) {
      try {
        if (!testMetadataMap.isEmpty()) {
          log.debug("Test meta data map {}", testMetadataMap);
          new MetadataPersister(outputFolder).persist(testMetadataMap);
          break;
        }
        Thread.sleep(1);
      } catch (Exception e) {
        ignoredOnPurpose(e);
      }
    }
  }
}
