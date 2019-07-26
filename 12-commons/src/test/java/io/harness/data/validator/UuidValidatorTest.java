package io.harness.data.validator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.utils.UuidUtils.base64StrToUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.harness.category.element.UnitTests;
import io.harness.utils.UuidUtils;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.UUID;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

@Slf4j
public class UuidValidatorTest {
  @Builder
  static class UuidValidatorTestStructure {
    @Uuid String str;
  }

  @Test
  @Category(UnitTests.class)
  public void testUuid() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    final Validator validator = factory.getValidator();

    // Some random string
    assertThat(validator.validate(UuidValidatorTestStructure.builder().str("abcd").build())).isNotEmpty();

    // Random UUID
    String accountId = UUID.randomUUID().toString();
    assertThat(validator.validate(UuidValidatorTestStructure.builder().str(accountId).build())).isEmpty();

    // Specific UUID
    assertThat(
        validator.validate(UuidValidatorTestStructure.builder().str("cdaed56d-8712-414d-b346-01905d0026fe").build()))
        .isEmpty();

    // Specific Base64 encoded UUID
    String base64Str = "za7VbYcSQU2zRgGQXQAm/g"; // a base64 encoded UUID
    String decodedUUIDStr = base64StrToUuid(base64Str);
    assertEquals("cdaed56d-8712-414d-b346-01905d0026fe", decodedUUIDStr);
    assertThat(validator.validate(UuidValidatorTestStructure.builder().str(base64Str).build())).isEmpty();

    // Random Base64 encoded UUID that is URL-safe
    String base64encodedUuid = generateUuid();
    assertThat(validator.validate(UuidValidatorTestStructure.builder().str(base64encodedUuid).build())).isEmpty();

    // Specific Base64 encoded UUID that is URL-safe
    String base64encodedUrlSafeUuid = "sXfoYJRPTOiIaqpICi_aUg";
    assertThat(validator.validate(UuidValidatorTestStructure.builder().str(base64encodedUrlSafeUuid).build()))
        .isEmpty();

    String uuidType1 = "efee4cba-9d5f-11e9-a2a3-2a2ae2dbcce4";
    assertTrue(UuidUtils.isValidUuidStr(uuidType1));

    String uuidType4 = "3bcd1e59-1dab-4f6f-a374-17b8e2339f64";
    assertTrue(UuidUtils.isValidUuidStr(uuidType4));
  }
}
