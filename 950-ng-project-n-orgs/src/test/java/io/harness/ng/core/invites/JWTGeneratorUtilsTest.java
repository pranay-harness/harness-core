package io.harness.ng.core.invites;

import static io.harness.ng.core.invites.entities.Invite.InviteKeys.id;
import static io.harness.rule.OwnerRule.ANKUSH;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.junit.Assert.assertTrue;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.auth0.jwt.interfaces.Claim;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class JWTGeneratorUtilsTest extends CategoryTest {
  private JWTGeneratorUtils jwtGeneratorUtils = new JWTGeneratorUtils();

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testGenerateAndVerityJWTToken() {
    String inviteId = randomAlphabetic(10);
    String jwtPasswordSecret = randomAlphabetic(20);
    Map<String, String> claims = ImmutableMap.of(id, inviteId);

    String jwtToken =
        jwtGeneratorUtils.generateJWTToken(claims, TimeUnit.MILLISECONDS.convert(60, TimeUnit.DAYS), jwtPasswordSecret);
    Map<String, Claim> returnClaims = jwtGeneratorUtils.verifyJWTToken(jwtToken, jwtPasswordSecret);
    assertTrue(returnClaims.containsKey(id));
  }
}
