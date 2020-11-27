package io.harness.overlayinputset;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonTypeName("overlayInputSet")
public interface OverlayInputSetWrapper {}
