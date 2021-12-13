package io.harness.cvng.servicelevelobjective.beans;

import java.util.List;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SLODashboardApiFilter {
  @QueryParam("userJourneyIdentifiers") List<String> userJourneyIdentifiers;
  @QueryParam("monitoredServiceIdentifier") String monitoredServiceIdentifier;
}
