/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.security;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class SignVerifierTest extends CategoryTest {
  private static JarFile jarFileFromResource(String location) throws IOException {
    URL resource = IOUtils.resourceToURL(location);
    File tmp = File.createTempFile("test-sign-verifier-", ".jar");
    tmp.deleteOnExit();
    FileUtils.copyURLToFile(resource, tmp);
    return new JarFile(tmp);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testNonSigned() throws IOException {
    try (JarFile jar = jarFileFromResource("/io/harness/security/non-signed.jar")) {
      assertThat(SignVerifier.verify(jar)).isTrue();
      assertThat(SignVerifier.meticulouslyVerify(jar)).isFalse();
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testRegularlySigned() throws IOException {
    try (JarFile jar = jarFileFromResource("/io/harness/security/regularly-signed.jar")) {
      assertThat(SignVerifier.verify(jar)).isTrue();
      assertThat(SignVerifier.meticulouslyVerify(jar)).isFalse();
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testModified() throws IOException {
    try (JarFile jar = jarFileFromResource("/io/harness/security/modified.jar")) {
      assertThat(SignVerifier.verify(jar)).isFalse();
      assertThat(SignVerifier.meticulouslyVerify(jar)).isFalse();
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testHarnessSigned() throws IOException {
    try (JarFile jar = jarFileFromResource("/io/harness/security/harness-signed.jar")) {
      assertThat(SignVerifier.verify(jar)).isTrue();
      assertThat(SignVerifier.meticulouslyVerify(jar)).isTrue();
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testHarnessSignedWithTheBazelRule() throws IOException {
    try (JarFile jar = jarFileFromResource("/980-commons/io/harness/security/non-signed_signed.jar")) {
      assertThat(SignVerifier.verify(jar)).isTrue();
      assertThat(SignVerifier.meticulouslyVerify(jar)).isTrue();
    }
  }
}
