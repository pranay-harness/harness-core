/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.security;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.CommonsMethodRule;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class AsymmetricTest extends CategoryTest {
  @Rule public CommonsMethodRule commonsMethodRule = new CommonsMethodRule();

  @Inject AsymmetricDecryptor asymmetricDecryptor;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEncoding()
      throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException,
             InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
    final byte[] encryptText = new AsymmetricEncryptor().encryptText("foo");
    assertThat(asymmetricDecryptor.decryptText(encryptText)).isEqualTo("foo");
  }
}
