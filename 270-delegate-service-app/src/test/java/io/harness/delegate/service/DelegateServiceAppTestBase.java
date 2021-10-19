package io.harness.delegate.service;

import io.harness.CategoryTest;
import io.harness.delegate.app.DelegateServiceApplication;
import io.harness.resource.Project;
import io.harness.rule.LifecycleRule;

import io.dropwizard.testing.ResourceHelpers;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Slf4j
public abstract class DelegateServiceAppTestBase extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();

  private static boolean isBazelTest() {
    return System.getProperty("user.dir").contains("/bin/");
  }

  public static String getResourceFilePath(String filePath) {
    return isBazelTest() ? "270-delegate-service-app/src/test/resources/" + filePath
                         : ResourceHelpers.resourceFilePath(filePath);
  }

  public static String getSourceResourceFile(Class clazz, String filePath) {
    return isBazelTest() ? "270-delegate-service-app/src/main/resources" + filePath
                         : clazz.getResource(filePath).getFile();
  }

  public static String getAppResourceFilePath(String filePath) {
    String absolutePath = Project.moduleDirectory(DelegateServiceAppTestBase.class);
    return isBazelTest() ? String.valueOf(absolutePath + "/" + filePath)
            : ResourceHelpers.resourceFilePath(filePath);
  }
}
