package io.harness.cvng.core.beans.monitoredService.changeSourceSpec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ChangeSourceSpec {}
