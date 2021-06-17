package io.harness.ccm.remote.resources;

import com.epam.reportportal.junit5.ReportPortalExtension;
import io.harness.category.element.UnitTests;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ReportPortalExtension.class)
@Slf4j
public class SampleTest {
  @Test
  @Category(UnitTests.class)
  void testMySimpleTest() {
    log.info("Hello from my simple test");
  }
  @Test
  @Category(UnitTests.class)
  void testMySimpleTest4() {
    Assert.assertTrue(1 == 1);
  }
}