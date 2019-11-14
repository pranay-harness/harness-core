package software.wings.beans.artifact;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.artifact.AmiArtifactStream.FilterClass;
import software.wings.beans.artifact.AmiArtifactStream.Tag;

public class AmiArtifactStreamTest extends CategoryTest {
  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testSourceName() {
    AmiArtifactStream artifactStream = new AmiArtifactStream();
    artifactStream.setRegion("TestRegion");
    assertThat(artifactStream.generateSourceName()).isEqualTo("TestRegion");

    FilterClass f1 = new FilterClass();
    f1.setKey("TestKey");
    f1.setValue("TestValue");

    FilterClass f2 = new FilterClass();
    f2.setKey("TestKey");
    f2.setValue("TestValue");

    artifactStream.setFilters(asList(f1, f2));

    assertThat(artifactStream.generateSourceName()).isEqualTo("TestRegion:TestKey:TestValue_TestKey:TestValue");

    Tag t1 = new Tag();
    t1.setKey("TestTagKey");
    t1.setValue("TestTagValue");

    Tag t2 = new Tag();
    t2.setKey("TestTagKey");
    t2.setValue("TestTagValue");

    artifactStream.setTags(asList(t1, t2));

    assertThat(artifactStream.generateSourceName())
        .isEqualTo("TestRegion:TestTagKey:TestTagValue_TestTagKey:TestTagValue:TestKey:TestValue_TestKey:TestValue");

    artifactStream.setFilters(null);

    assertThat(artifactStream.generateSourceName())
        .isEqualTo("TestRegion:TestTagKey:TestTagValue_TestTagKey:TestTagValue");
  }
}
