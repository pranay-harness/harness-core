package io.harness.yaml.core.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.beans.WithIdentifier;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonTypeName("inputSet")
public interface BaseInputSetConfig extends WithIdentifier {}
