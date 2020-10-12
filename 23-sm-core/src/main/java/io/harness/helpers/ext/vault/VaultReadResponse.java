package io.harness.helpers.ext.vault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Created by rsingh on 11/3/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class VaultReadResponse {
  private Map<String, String> data;
}
