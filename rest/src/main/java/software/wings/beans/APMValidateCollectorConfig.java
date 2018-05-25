package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class APMValidateCollectorConfig {
  private String baseUrl;
  private String url;
  private Map<String, String> headers;
  private Map<String, String> options;
  List<EncryptedDataDetail> encryptedDataDetails;
}
