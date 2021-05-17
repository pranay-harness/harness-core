package io.harness.delegate.utils;

import static io.harness.rule.OwnerRule.MARKO;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.DelegateServiceTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.DEL)
public class DelegateEntityOwnerMapperTest extends DelegateServiceTestBase {
  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testBuildOwner() {
    assertThat(DelegateEntityOwnerMapper.buildOwner(null, null)).isNull();
    assertThat(DelegateEntityOwnerMapper.buildOwner("", "")).isNull();
    assertThat(DelegateEntityOwnerMapper.buildOwner("o1", null))
        .isEqualTo(DelegateEntityOwner.builder().identifier("o1").build());
    assertThat(DelegateEntityOwnerMapper.buildOwner(null, "p1"))
        .isEqualTo(DelegateEntityOwner.builder().identifier("p1").build());
    assertThat(DelegateEntityOwnerMapper.buildOwner("o1", "p1"))
        .isEqualTo(DelegateEntityOwner.builder().identifier("o1/p1").build());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testExtractOrgIdFromOwnerIdentifier() {
    assertThat(DelegateEntityOwnerMapper.extractOrgIdFromOwnerIdentifier(null)).isNull();
    assertThat(DelegateEntityOwnerMapper.extractOrgIdFromOwnerIdentifier("")).isNull();
    assertThat(DelegateEntityOwnerMapper.extractOrgIdFromOwnerIdentifier("o1")).isEqualTo("o1");
    assertThat(DelegateEntityOwnerMapper.extractOrgIdFromOwnerIdentifier("o1/")).isEqualTo("o1");
    assertThat(DelegateEntityOwnerMapper.extractOrgIdFromOwnerIdentifier("o1/p1")).isEqualTo("o1");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testExtractProjectIdFromOwnerIdentifier() {
    assertThat(DelegateEntityOwnerMapper.extractProjectIdFromOwnerIdentifier(null)).isNull();
    assertThat(DelegateEntityOwnerMapper.extractProjectIdFromOwnerIdentifier("")).isNull();
    assertThat(DelegateEntityOwnerMapper.extractProjectIdFromOwnerIdentifier("o1")).isNull();
    assertThat(DelegateEntityOwnerMapper.extractProjectIdFromOwnerIdentifier("o1/")).isNull();
    assertThat(DelegateEntityOwnerMapper.extractProjectIdFromOwnerIdentifier("o1/p1")).isEqualTo("p1");
  }
}
