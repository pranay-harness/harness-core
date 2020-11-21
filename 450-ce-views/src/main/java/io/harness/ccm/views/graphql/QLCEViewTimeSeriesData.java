package io.harness.ccm.views.graphql;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLCEViewTimeSeriesData {
  List<QLCEViewDataPoint> values;
  Long time;
  String date;
}
