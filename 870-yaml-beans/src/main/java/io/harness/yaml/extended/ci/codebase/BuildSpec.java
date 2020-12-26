package io.harness.yaml.extended.ci.codebase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface BuildSpec {}
