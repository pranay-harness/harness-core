package io.harness.serviceaccount;

import io.harness.beans.Scope;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceAccountDTO {
  String name;
  Scope scope;
}
