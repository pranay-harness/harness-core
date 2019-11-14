package software.wings.security;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.auth0.jwt.interfaces.Claim;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.security.SecretManager.JWT_CATEGORY;

import java.util.Map;

public class SecretManagerTest extends WingsBaseTest {
  private static final String CLAIM_VALUE = "john.doe@harness.io";
  private static final String CLAIM_KEY = "email";
  @Inject private SecretManager secretManager;

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldGenerateToken() {
    String token =
        secretManager.generateJWTToken(ImmutableMap.of(CLAIM_KEY, CLAIM_VALUE), JWT_CATEGORY.EXTERNAL_SERVICE_SECRET);
    assertThat(token).isNotEmpty();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldVerifyToken() {
    String jwtToken =
        secretManager.generateJWTToken(ImmutableMap.of(CLAIM_KEY, CLAIM_VALUE), JWT_CATEGORY.EXTERNAL_SERVICE_SECRET);
    Map<String, Claim> claimMap = secretManager.verifyJWTToken(jwtToken, JWT_CATEGORY.EXTERNAL_SERVICE_SECRET);
    assertThat(claimMap).isNotEmpty().containsKey(CLAIM_KEY);
    assertThat(claimMap.get(CLAIM_KEY).asString()).isEqualTo(CLAIM_VALUE);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhenNoTokenFoundOnValidation() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> secretManager.verifyJWTToken("INVALID_TOKEN", JWT_CATEGORY.EXTERNAL_SERVICE_SECRET));
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhenResourceDoesntMatchOnValiation() {
    String token = secretManager.generateJWTToken(null, JWT_CATEGORY.EXTERNAL_SERVICE_SECRET);
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> secretManager.verifyJWTToken(token, JWT_CATEGORY.AUTH_SECRET));
  }
}
