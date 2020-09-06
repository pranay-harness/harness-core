package io.harness.delegate.beans;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.tasks.ResponseData;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface DelegateResponseData extends ResponseData {}
