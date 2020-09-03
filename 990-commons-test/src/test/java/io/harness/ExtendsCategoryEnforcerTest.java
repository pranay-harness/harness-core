package io.harness;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.Description;
import org.powermock.core.classloader.MockClassLoader;

public class ExtendsCategoryEnforcerTest extends CategoryTest {
  private ExtendsCategoryEnforcer extendsCategoryEnforcer;

  @Before
  public void setUp() throws Exception {
    extendsCategoryEnforcer = new ExtendsCategoryEnforcer();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldNotFailCompliantTest() throws Exception {
    assertThatCode(
        () -> extendsCategoryEnforcer.testStarted(Description.createTestDescription(CompliantTest.class, "testName")))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldFailNonCompliantTest() throws Exception {
    assertThatExceptionOfType(AssertionError.class)
        .isThrownBy(()
                        -> extendsCategoryEnforcer.testStarted(
                            Description.createTestDescription(NonCompliantTest.class, "testName")))
        .withMessage("Test classes should extend CategoryTest");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldNotFailCompliantTestFromDifferentClassloader() throws Exception {
    ClassLoader powermockCl = new MockClassLoader(null, null, null);
    assertThatCode(()
                       -> extendsCategoryEnforcer.testStarted(Description.createTestDescription(
                           powermockCl.loadClass(CompliantTest.class.getName()), "testName")))
        .doesNotThrowAnyException();
  }

  private static class CompliantTest extends CategoryTest {}

  private static class NonCompliantTest {}
}