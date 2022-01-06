/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.security;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

@Slf4j
public class SimpleEncryptionTest extends CategoryTest {
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void shouldEncryptAndDecrypt() {
    String testInput = "abc";
    SimpleEncryption encryption = new SimpleEncryption();
    byte[] encryptedBytes = encryption.encrypt(testInput.getBytes(StandardCharsets.ISO_8859_1));
    String encryptedString = new String(encryptedBytes, StandardCharsets.ISO_8859_1);
    assertThat(testInput).isNotEqualTo(encryptedString);
    byte[] decryptedBytes = encryption.decrypt(encryptedBytes);
    String decryptedString = new String(decryptedBytes, StandardCharsets.ISO_8859_1);
    assertThat(testInput).isEqualTo(decryptedString);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void shouldEncryptAndDecryptWithCustomKey() {
    char[] KEY = "abcdefghijklmnopabcdefghijklmnop".toCharArray();
    String testInput = "abc";
    SimpleEncryption encryption = new SimpleEncryption(KEY);
    byte[] encryptedBytes = encryption.encrypt(testInput.getBytes(StandardCharsets.ISO_8859_1));
    String encryptedString = new String(encryptedBytes, StandardCharsets.ISO_8859_1);
    assertThat(testInput).isNotEqualTo(encryptedString);
    byte[] decryptedBytes = encryption.decrypt(encryptedBytes);
    String decryptedString = new String(decryptedBytes, StandardCharsets.ISO_8859_1);
    assertThat(testInput).isEqualTo(decryptedString);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncryptDecryptCharsWithCustomKey() {
    String testInput = "test";
    SimpleEncryption encryption = new SimpleEncryption("kmpySmUISimoRrJL6NL73w");
    char[] encryptedChars = encryption.encryptChars(testInput.toCharArray());
    String encryptedString = new String(encryptedChars);
    assertThat(testInput).isNotEqualTo(encryptedString);
    char[] decryptedChars = encryption.decryptChars(encryptedString.toCharArray());
    String decryptedString = new String(decryptedChars);
    log.info("decryptedString: {}", decryptedString);
    assertThat(testInput).isEqualTo(decryptedString);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void shouldFailWithIncorrectKeyLength() {
    thrown.expect(WingsException.class);
    thrown.expectMessage(EncryptionUtils.DEFAULT_SALT_SIZE + " characters");
    char[] KEY = "abc".toCharArray();
    String testInput = "abc";
    SimpleEncryption encryption = new SimpleEncryption(KEY);
    encryption.encrypt(testInput.getBytes(StandardCharsets.ISO_8859_1));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldHaveJCEEnabled() {
    try {
      int maxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
      assertThat(maxKeyLength).isEqualTo(2147483647);
    } catch (NoSuchAlgorithmException exception) {
      log.error("", exception);
    }
  }
}
