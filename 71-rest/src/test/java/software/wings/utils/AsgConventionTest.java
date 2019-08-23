package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AsgConventionTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testGetRevisionFromTag() {
    int revision = AsgConvention.getRevisionFromTag("aaa__123");
    assertThat(revision).isEqualTo(123);

    revision = AsgConvention.getRevisionFromTag("aaa__bbb__ccc__123");
    assertThat(revision).isEqualTo(123);

    revision = AsgConvention.getRevisionFromTag("aaabbbccc");
    assertThat(revision).isEqualTo(0);

    revision = AsgConvention.getRevisionFromTag("aaa__bbb__ccc");
    assertThat(revision).isEqualTo(0);

    revision = AsgConvention.getRevisionFromTag(null);
    assertThat(revision).isEqualTo(0);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetAsgNamePrefix() {
    String asgNamePrefix = AsgConvention.getAsgNamePrefix("appName", "serviceName", "envName");
    assertThat(asgNamePrefix).isEqualTo("appName__serviceName__envName");

    asgNamePrefix = AsgConvention.getAsgNamePrefix("app&Name", "service+Name", "env*Name");
    assertThat(asgNamePrefix).isEqualTo("app__Name__service__Name__env__Name");

    asgNamePrefix = AsgConvention.getAsgNamePrefix("app/Name", "service.Name", "env'Name");
    assertThat(asgNamePrefix).isEqualTo("app__Name__service__Name__env__Name");

    asgNamePrefix = AsgConvention.getAsgNamePrefix("app$Name", "service Name", "env\"Name");
    assertThat(asgNamePrefix).isEqualTo("app__Name__service__Name__env__Name");

    asgNamePrefix = AsgConvention.getAsgNamePrefix("app$Name", "service|Name", "env\\Name");
    assertThat(asgNamePrefix).isEqualTo("app__Name__service__Name__env__Name");

    asgNamePrefix = AsgConvention.getAsgNamePrefix("appName", null, null);
    assertThat(asgNamePrefix).isEqualTo("appName__null__null");
  }
}
