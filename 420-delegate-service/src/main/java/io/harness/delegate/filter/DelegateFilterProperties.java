package io.harness.delegate.filter;

import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.filter.entity.FilterProperties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("io.harness.delegate.filter.DelegateFilterProperties")
public class DelegateFilterProperties extends FilterProperties {
  private DelegateInstanceStatus status;
  private String description;
  private String hostName;
  private String delegateName;
  private String delegateType;
  private String delegateGroupIdentifier;
}