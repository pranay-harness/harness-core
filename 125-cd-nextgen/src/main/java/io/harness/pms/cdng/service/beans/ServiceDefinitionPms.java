package io.harness.pms.cdng.service.beans;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.pms.cdng.service.ServiceSpecPms;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("serviceDefinitionPms")
public class ServiceDefinitionPms {
  String uuid;
  String type;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  ServiceSpecPms serviceSpecPms;

  // Use Builder as Constructor then only external property(visible) will be filled.
  @Builder
  public ServiceDefinitionPms(String type, ServiceSpecPms serviceSpecPms) {
    this.type = type;
    this.serviceSpecPms = serviceSpecPms;
  }
}
