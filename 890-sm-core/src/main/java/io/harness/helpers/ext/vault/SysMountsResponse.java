/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.helpers.ext.vault;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
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
@OwnedBy(PL)
public class SysMountsResponse {
  @Default private Map<String, SysMount> data = new HashMap<>();
}
