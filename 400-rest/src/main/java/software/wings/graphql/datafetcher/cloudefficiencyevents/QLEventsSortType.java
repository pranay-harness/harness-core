package software.wings.graphql.datafetcher.cloudefficiencyevents;

import software.wings.graphql.datafetcher.cloudefficiencyevents.CEEventsQueryMetaData.CEEventsMetaDataFields;

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
