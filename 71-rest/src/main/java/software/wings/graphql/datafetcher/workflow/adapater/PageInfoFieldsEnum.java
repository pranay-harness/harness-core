package software.wings.graphql.datafetcher.workflow.adapater;

import lombok.Getter;

public enum PageInfoFieldsEnum {
  TOTAL("total"),
  OFFSET("offset"),
  LIMIT("limit");

  @Getter private String fieldName;

  PageInfoFieldsEnum(String fieldName) {
    this.fieldName = fieldName;
  }
}
