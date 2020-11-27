package io.harness.yaml.core.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
public interface OverrideSetsWrapper extends WithIdentifier {}
