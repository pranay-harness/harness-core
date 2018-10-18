package io.harness.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.ModelTest;
import io.harness.scm.SecretName;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class SecretGeneratorTest extends ModelTest {
  private static final Logger logger = LoggerFactory.getLogger(SecretGeneratorTest.class);

  @Inject SecretGenerator secretGenerator;

  @Test
  public void testStore() {
    final SecretName secretKey = SecretName.builder().value("secret_key").build();

    String id1 = secretGenerator.ensureStored(GLOBAL_ACCOUNT_ID, secretKey);
    String id2 = secretGenerator.ensureStored(GLOBAL_ACCOUNT_ID, secretKey);
    assertThat(id1).isEqualTo(id2);
  }
}
