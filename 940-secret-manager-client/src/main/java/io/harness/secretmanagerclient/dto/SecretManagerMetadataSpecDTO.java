package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "encryptionType",
    visible = true)
public abstract class SecretManagerMetadataSpecDTO {}
