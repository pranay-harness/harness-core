/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.schema.client;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import io.harness.yaml.schema.client.config.YamlSchemaClientConfig;
import io.harness.yaml.schema.client.config.YamlSchemaHttpClientConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(DX)
public class YamlSchemaClientModule extends AbstractModule {
  private final YamlSchemaClientConfig yamlSchemaClientConfig;
  private final String clientId;

  private static YamlSchemaClientModule instance;

  private YamlSchemaClientModule(YamlSchemaClientConfig yamlSchemaClientConfig, String clientId) {
    this.yamlSchemaClientConfig = yamlSchemaClientConfig;
    this.clientId = clientId;
  }

  public static YamlSchemaClientModule getInstance(YamlSchemaClientConfig yamlSchemaClientConfig, String clientId) {
    if (instance == null) {
      instance = new YamlSchemaClientModule(yamlSchemaClientConfig, clientId);
    }

    return instance;
  }

  @Override
  protected void configure() {}

  @Provides
  @Singleton
  private Map<String, YamlSchemaClient> yamlSchemaClientMap(KryoConverterFactory kryoConverterFactory) {
    Map<String, YamlSchemaHttpClientConfig> yamlSchemaHttpClientMap =
        yamlSchemaClientConfig.getYamlSchemaHttpClientMap();

    Map<String, YamlSchemaClient> map = new HashMap<>();
    yamlSchemaHttpClientMap.forEach(
        (k, v)
            -> map.put(k,
                new YamlSchemaHttpClientFactory(v.getServiceHttpClientConfig(), v.getSecret(),
                    new ServiceTokenGenerator(), kryoConverterFactory, this.clientId)
                    .create()));

    return map;
  }
}
