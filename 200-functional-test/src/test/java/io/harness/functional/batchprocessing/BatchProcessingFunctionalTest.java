package io.harness.functional.batchprocessing;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.CategoryTest;
import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import io.harness.testframework.framework.BatchProcessingExecutor;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BatchProcessingFunctionalTest extends CategoryTest {
  private static final String ALPN_JAR =
      "org/mortbay/jetty/alpn/alpn-boot/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar";
  private static final String ALPN = "/home/jenkins/maven-repositories/0/";

  private static final BatchProcessingExecutor batchProcessingExecutor = new BatchProcessingExecutor();

  @Test
  @Owner(developers = AVMOHAN)
  @Category(FunctionalTests.class)
  public void shouldEnsureBatchProcessingStartsUp() throws Exception {
    assertThatCode(() -> batchProcessingExecutor.ensureBatchProcessing(getClass(), ALPN, ALPN_JAR))
        .doesNotThrowAnyException();
  }
}
