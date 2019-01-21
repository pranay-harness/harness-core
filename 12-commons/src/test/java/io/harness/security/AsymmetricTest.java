package io.harness.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.rule.CommonsMethodRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class AsymmetricTest extends CategoryTest {
  private static final Logger logger = LoggerFactory.getLogger(AsymmetricTest.class);

  @Rule public CommonsMethodRule commonsMethodRule = new CommonsMethodRule();

  @Inject AsymmetricDecryptor asymmetricDecryptor;

  @Test
  public void testEncoding()
      throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException,
             InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
    final byte[] encryptText = new AsymmetricEncryptor().encryptText("foo");
    assertThat(asymmetricDecryptor.decryptText(encryptText)).isEqualTo("foo");
  }
}
