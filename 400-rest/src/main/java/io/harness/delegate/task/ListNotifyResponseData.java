/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.persistence.PersistentEntity;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ListNotifyResponseData implements DelegateTaskNotifyResponseData {
  private List<PersistentEntity> data = new ArrayList<>();
  private DelegateMetaInfo delegateMetaInfo;

  public List getData() {
    return data;
  }

  /**
   * Adds data.
   *
   * @param data the data
   */
  public void addData(PersistentEntity data) {
    this.data.add(data);
  }

  /**
   * Sets data.
   *
   * @param data the data
   */
  public void setData(List data) {
    this.data = data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ListNotifyResponseData that = (ListNotifyResponseData) o;

    return data != null ? data.equals(that.data) : that.data == null;
  }

  @Override
  public int hashCode() {
    return data != null ? data.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "ListNotifyResponseData{"
        + "data=" + data + '}';
  }

  public static final class Builder {
    private List<PersistentEntity> data = new ArrayList<>();

    private Builder() {}

    public static Builder aListNotifyResponseData() {
      return new Builder();
    }

    public Builder addData(PersistentEntity data) {
      this.data.add(data);
      return this;
    }

    public ListNotifyResponseData build() {
      ListNotifyResponseData listNotifyResponseData = new ListNotifyResponseData();
      listNotifyResponseData.setData(data);
      return listNotifyResponseData;
    }
  }
}
