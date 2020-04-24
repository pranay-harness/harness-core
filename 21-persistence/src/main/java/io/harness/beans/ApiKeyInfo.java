package io.harness.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyInfo {
  String appKeyId;
  String apiKeyName;

  public static EmbeddedUser getEmbeddedUserFromApiKey(ApiKeyInfo apiKeyInfo) {
    return EmbeddedUser.builder().name(apiKeyInfo.getApiKeyName() + " (Api Key)").build();
  }
}
