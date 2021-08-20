package io.harness.cvng.core.beans.monitoredService.changeSourceSpec;

import static io.harness.cvng.CVConstants.DATA_SOURCE_TYPE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = HarnessCDChangeSourceSpec.class, name = "HarnessCD") })
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = DATA_SOURCE_TYPE, include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
public abstract class ChangeSourceSpec {}
