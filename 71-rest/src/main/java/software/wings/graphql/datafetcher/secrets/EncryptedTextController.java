package software.wings.graphql.datafetcher.secrets;

import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.schema.type.secrets.QLEncryptedText;
import software.wings.security.encryption.EncryptedData;

@Slf4j
@Singleton
public class EncryptedTextController {
  public QLEncryptedText populateEncryptedText(EncryptedData encryptedText) {
    return QLEncryptedText.builder()
        .id(encryptedText.getUuid())
        .secretManagerId(encryptedText.getKmsId())
        .name(encryptedText.getName())
        .build();
  }
}
