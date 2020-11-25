package io.harness.delegate.beans;

import io.harness.tasks.ResponseData;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface DelegateResponseData extends ResponseData {}
