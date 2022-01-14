package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "accessType", visible = true)
public abstract class VaultCredentialDTO {}
