package software.wings.service.impl.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.wings.service.impl.security.SecretManagerImpl.ENCRYPTED_FIELD_MASK;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.security.SecretManager;

public class SecretManagerTest extends WingsBaseTest {
  @Inject @InjectMocks private SecretManager secretManager;
  @Mock private WingsPersistence wingsPersistence;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }
  @Test
  public void testDecryptYamlRef() throws Exception {
    try {
      secretManager.decryptYamlRef(null);
      fail("Exception expected");
    } catch (IllegalStateException e) {
      assertTrue(true);
    }

    try {
      secretManager.decryptYamlRef("test:123");
      fail("Exception expected");
    } catch (WingsException e) {
      assertEquals("encryptedData is null", e.getMessage());
    }
  }

  @Test
  public void testMaskEncryptedFields() {
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build();
    secretManager.maskEncryptedFields(awsConfig);
    assertArrayEquals(awsConfig.getSecretKey(), ENCRYPTED_FIELD_MASK);
  }

  @Test
  public void testResetUnchangedEncryptedFields() {
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build();
    AwsConfig maskedAwsConfig =
        AwsConfig.builder().accountId(ACCOUNT_ID).accessKey(ACCESS_KEY).secretKey(ENCRYPTED_FIELD_MASK).build();
    secretManager.resetUnchangedEncryptedFields(awsConfig, maskedAwsConfig);
    assertArrayEquals(maskedAwsConfig.getSecretKey(), SECRET_KEY);
  }
}
