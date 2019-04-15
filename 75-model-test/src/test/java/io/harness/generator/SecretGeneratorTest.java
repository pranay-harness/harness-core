package io.harness.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.ModelTest;
import io.harness.category.element.UnitTests;
import io.harness.scm.SecretName;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Ignore
@Slf4j
public class SecretGeneratorTest extends ModelTest {
  @Inject SecretGenerator secretGenerator;

  @Test
  @Category(UnitTests.class)
  public void testStore() {
    final SecretName secretKey = SecretName.builder().value("secret_key").build();

    String id1 = secretGenerator.ensureStored(GLOBAL_ACCOUNT_ID, secretKey);
    String id2 = secretGenerator.ensureStored(GLOBAL_ACCOUNT_ID, secretKey);
    assertThat(id1).isEqualTo(id2);
  }
}
