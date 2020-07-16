package io.harness.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NGPageResponse<T> {
  long totalPages;
  long totalElements;
  long size;
  List<T> content;
  long pageNumber;
  boolean empty;
}
