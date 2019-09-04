package software.wings.service.impl.security.vault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author marklu on 9/3/19
 *
 * Parsing the /secret/option/version integer value of the of full sys mounts output JSON from the
 * Vault /v1/secret/sys/mounts REST API call. Sample snippet of the output call is:
 * <p>
 * {
 * "secret/": {
 * "accessor": "kv_7fa3b4ad",
 * "config": {
 * "default_lease_ttl": 0,
 * "force_no_cache": false,
 * "max_lease_ttl": 0,
 * "plugin_name": ""
 * },
 * "description": "key\/value secret storage",
 * "local": false,
 * "options": {
 * "version": "2"
 * },
 * "seal_wrap": false,
 * "type": "kv"
 * }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysMountsResponse {
  @Default private Map<String, SysMount> data = new HashMap<>();
}
