package io.harness.cvng.core.beans.monitoredService.changeSourceSpec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarnessCDChangeSourceSpec extends ChangeSourceSpec {}
