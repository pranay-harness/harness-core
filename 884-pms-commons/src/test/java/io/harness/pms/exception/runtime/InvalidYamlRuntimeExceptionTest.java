/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.exception.runtime;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class InvalidYamlRuntimeExceptionTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testConstructors() throws IOException {
    InvalidYamlRuntimeException justAMessage = new InvalidYamlRuntimeException("Just a message");
    assertThat(justAMessage.getMessage()).isEqualTo("Just a message");
    assertThat(justAMessage.getOriginalException()).isNull();
    assertThat(justAMessage.getYamlNode()).isNull();

    String yaml = "pipeline: p1";
    YamlNode yamlNode = YamlUtils.readTree(yaml).getNode();
    InvalidYamlRuntimeException withYamlNode = new InvalidYamlRuntimeException("m1", yamlNode);
    assertThat(withYamlNode).hasMessage("m1");
    assertThat(withYamlNode.getYamlNode()).isEqualTo(yamlNode);
    assertThat(withYamlNode.getOriginalException()).isNull();

    IOException ioException = new IOException("something");
    InvalidYamlRuntimeException withOriginalException = new InvalidYamlRuntimeException("m2", ioException);
    assertThat(withOriginalException).hasMessage("m2");
    assertThat(withOriginalException.getYamlNode()).isNull();
    assertThat(withOriginalException.getOriginalException()).isEqualTo(ioException);

    InvalidYamlRuntimeException withEverything = new InvalidYamlRuntimeException("m3", ioException, yamlNode);
    assertThat(withEverything).hasMessage("m3");
    assertThat(withEverything.getYamlNode()).isEqualTo(yamlNode);
    assertThat(withEverything.getOriginalException()).isEqualTo(ioException);
  }
}
