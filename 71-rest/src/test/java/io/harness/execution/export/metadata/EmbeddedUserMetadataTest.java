package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EmbeddedUserMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromEmbeddedUser() {
    assertThat(EmbeddedUserMetadata.fromEmbeddedUser(null)).isNull();
    assertThat(EmbeddedUserMetadata.fromEmbeddedUser(EmbeddedUser.builder().build())).isNull();
    EmbeddedUserMetadata embeddedUserMetadata =
        EmbeddedUserMetadata.fromEmbeddedUser(EmbeddedUser.builder().name("n").email("e").build());
    assertThat(embeddedUserMetadata).isNotNull();
    assertThat(embeddedUserMetadata.getName()).isEqualTo("n");
    assertThat(embeddedUserMetadata.getEmail()).isEqualTo("e");
  }
}
