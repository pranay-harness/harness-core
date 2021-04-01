package software.wings.graphql.datafetcher.cloudefficiencyevents;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.cloudefficiencyevents.CEEventsQueryMetaData.CEEventsMetaDataFields;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CE)
public enum QLEventsSortType {
  Time(CEEventsMetaDataFields.STARTTIME),
  Cost(CEEventsMetaDataFields.BILLINGAMOUNT);
  private CEEventsMetaDataFields eventsMetaData;

  QLEventsSortType(CEEventsMetaDataFields eventsMetaData) {
    this.eventsMetaData = eventsMetaData;
  }

  public CEEventsMetaDataFields getEventsMetaData() {
    return eventsMetaData;
  }
}
