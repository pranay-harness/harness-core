package io.harness.ngtriggers.beans.config;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.beans.WithIdentifier;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonTypeName("trigger")
public interface NGTriggerInterface extends WithIdentifier {}
