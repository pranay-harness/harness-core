package io.harness.beans;

import io.harness.delegate.beans.DelegateTaskEvent;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DelegateTaskEventsResponse {
  List<DelegateTaskEvent> delegateTaskEvents;
}
