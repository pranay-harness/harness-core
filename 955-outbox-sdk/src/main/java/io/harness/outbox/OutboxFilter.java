package io.harness.outbox;

import io.harness.ng.beans.PageRequest;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OutboxFilter {
  List<String> outboxIds;
  List<String> types;
  PageRequest pageRequest;
}
