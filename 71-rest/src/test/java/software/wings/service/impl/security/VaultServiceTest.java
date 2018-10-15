package software.wings.service.impl.security;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * An unit test class for {@link VaultServiceImpl}.
 *
 * @author mark.lu on 10/11/18
 */
public class VaultServiceTest {
  private static final String SYS_MOUNTS_JSON_RESPONSE_V1 =
      "{\"secret/\":{\"accessor\":\"kv_290803b3\",\"description\":\"key/value secret storage\",\"type\":\"kv\"}}";
  private static final String SYS_MOUNTS_JSON_RESPONSE_V2 =
      "{\"secret/\":{\"accessor\":\"kv_290803b3\",\"description\":\"key/value secret storage\",\"options\": {\"version\": \"2\"},\"type\":\"kv\"}}";

  @Test
  public void testParsingVaultSysMountsResponseV1() {
    int version = VaultServiceImpl.parseSecretEngineVersionFromSysMountsJson(SYS_MOUNTS_JSON_RESPONSE_V1);
    assertEquals(1, version);
  }

  @Test
  public void testParsingVaultSysMountsResponseV2() {
    int version = VaultServiceImpl.parseSecretEngineVersionFromSysMountsJson(SYS_MOUNTS_JSON_RESPONSE_V2);
    assertEquals(2, version);
  }
}
