/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables.records;

import io.harness.timescaledb.tables.PodInfo;

import java.time.OffsetDateTime;
import org.jooq.Field;
import org.jooq.Record13;
import org.jooq.Row13;
import org.jooq.impl.TableRecordImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class PodInfoRecord extends TableRecordImpl<PodInfoRecord> implements Record13<String, String, String,
    OffsetDateTime, OffsetDateTime, String, String, String, Double, Double, String, OffsetDateTime, OffsetDateTime> {
  private static final long serialVersionUID = 1L;

  /**
   * Setter for <code>public.pod_info.accountid</code>.
   */
  public PodInfoRecord setAccountid(String value) {
    set(0, value);
    return this;
  }

  /**
   * Getter for <code>public.pod_info.accountid</code>.
   */
  public String getAccountid() {
    return (String) get(0);
  }

  /**
   * Setter for <code>public.pod_info.clusterid</code>.
   */
  public PodInfoRecord setClusterid(String value) {
    set(1, value);
    return this;
  }

  /**
   * Getter for <code>public.pod_info.clusterid</code>.
   */
  public String getClusterid() {
    return (String) get(1);
  }

  /**
   * Setter for <code>public.pod_info.instanceid</code>.
   */
  public PodInfoRecord setInstanceid(String value) {
    set(2, value);
    return this;
  }

  /**
   * Getter for <code>public.pod_info.instanceid</code>.
   */
  public String getInstanceid() {
    return (String) get(2);
  }

  /**
   * Setter for <code>public.pod_info.starttime</code>.
   */
  public PodInfoRecord setStarttime(OffsetDateTime value) {
    set(3, value);
    return this;
  }

  /**
   * Getter for <code>public.pod_info.starttime</code>.
   */
  public OffsetDateTime getStarttime() {
    return (OffsetDateTime) get(3);
  }

  /**
   * Setter for <code>public.pod_info.stoptime</code>.
   */
  public PodInfoRecord setStoptime(OffsetDateTime value) {
    set(4, value);
    return this;
  }

  /**
   * Getter for <code>public.pod_info.stoptime</code>.
   */
  public OffsetDateTime getStoptime() {
    return (OffsetDateTime) get(4);
  }

  /**
   * Setter for <code>public.pod_info.parentnodeid</code>.
   */
  public PodInfoRecord setParentnodeid(String value) {
    set(5, value);
    return this;
  }

  /**
   * Getter for <code>public.pod_info.parentnodeid</code>.
   */
  public String getParentnodeid() {
    return (String) get(5);
  }

  /**
   * Setter for <code>public.pod_info.namespace</code>.
   */
  public PodInfoRecord setNamespace(String value) {
    set(6, value);
    return this;
  }

  /**
   * Getter for <code>public.pod_info.namespace</code>.
   */
  public String getNamespace() {
    return (String) get(6);
  }

  /**
   * Setter for <code>public.pod_info.name</code>.
   */
  public PodInfoRecord setName(String value) {
    set(7, value);
    return this;
  }

  /**
   * Getter for <code>public.pod_info.name</code>.
   */
  public String getName() {
    return (String) get(7);
  }

  /**
   * Setter for <code>public.pod_info.cpurequest</code>.
   */
  public PodInfoRecord setCpurequest(Double value) {
    set(8, value);
    return this;
  }

  /**
   * Getter for <code>public.pod_info.cpurequest</code>.
   */
  public Double getCpurequest() {
    return (Double) get(8);
  }

  /**
   * Setter for <code>public.pod_info.memoryrequest</code>.
   */
  public PodInfoRecord setMemoryrequest(Double value) {
    set(9, value);
    return this;
  }

  /**
   * Getter for <code>public.pod_info.memoryrequest</code>.
   */
  public Double getMemoryrequest() {
    return (Double) get(9);
  }

  /**
   * Setter for <code>public.pod_info.workloadid</code>.
   */
  public PodInfoRecord setWorkloadid(String value) {
    set(10, value);
    return this;
  }

  /**
   * Getter for <code>public.pod_info.workloadid</code>.
   */
  public String getWorkloadid() {
    return (String) get(10);
  }

  /**
   * Setter for <code>public.pod_info.createdat</code>.
   */
  public PodInfoRecord setCreatedat(OffsetDateTime value) {
    set(11, value);
    return this;
  }

  /**
   * Getter for <code>public.pod_info.createdat</code>.
   */
  public OffsetDateTime getCreatedat() {
    return (OffsetDateTime) get(11);
  }

  /**
   * Setter for <code>public.pod_info.updatedat</code>.
   */
  public PodInfoRecord setUpdatedat(OffsetDateTime value) {
    set(12, value);
    return this;
  }

  /**
   * Getter for <code>public.pod_info.updatedat</code>.
   */
  public OffsetDateTime getUpdatedat() {
    return (OffsetDateTime) get(12);
  }

  // -------------------------------------------------------------------------
  // Record13 type implementation
  // -------------------------------------------------------------------------

  @Override
  public Row13<String, String, String, OffsetDateTime, OffsetDateTime, String, String, String, Double, Double, String,
      OffsetDateTime, OffsetDateTime>
  fieldsRow() {
    return (Row13) super.fieldsRow();
  }

  @Override
  public Row13<String, String, String, OffsetDateTime, OffsetDateTime, String, String, String, Double, Double, String,
      OffsetDateTime, OffsetDateTime>
  valuesRow() {
    return (Row13) super.valuesRow();
  }

  @Override
  public Field<String> field1() {
    return PodInfo.POD_INFO.ACCOUNTID;
  }

  @Override
  public Field<String> field2() {
    return PodInfo.POD_INFO.CLUSTERID;
  }

  @Override
  public Field<String> field3() {
    return PodInfo.POD_INFO.INSTANCEID;
  }

  @Override
  public Field<OffsetDateTime> field4() {
    return PodInfo.POD_INFO.STARTTIME;
  }

  @Override
  public Field<OffsetDateTime> field5() {
    return PodInfo.POD_INFO.STOPTIME;
  }

  @Override
  public Field<String> field6() {
    return PodInfo.POD_INFO.PARENTNODEID;
  }

  @Override
  public Field<String> field7() {
    return PodInfo.POD_INFO.NAMESPACE;
  }

  @Override
  public Field<String> field8() {
    return PodInfo.POD_INFO.NAME;
  }

  @Override
  public Field<Double> field9() {
    return PodInfo.POD_INFO.CPUREQUEST;
  }

  @Override
  public Field<Double> field10() {
    return PodInfo.POD_INFO.MEMORYREQUEST;
  }

  @Override
  public Field<String> field11() {
    return PodInfo.POD_INFO.WORKLOADID;
  }

  @Override
  public Field<OffsetDateTime> field12() {
    return PodInfo.POD_INFO.CREATEDAT;
  }

  @Override
  public Field<OffsetDateTime> field13() {
    return PodInfo.POD_INFO.UPDATEDAT;
  }

  @Override
  public String component1() {
    return getAccountid();
  }

  @Override
  public String component2() {
    return getClusterid();
  }

  @Override
  public String component3() {
    return getInstanceid();
  }

  @Override
  public OffsetDateTime component4() {
    return getStarttime();
  }

  @Override
  public OffsetDateTime component5() {
    return getStoptime();
  }

  @Override
  public String component6() {
    return getParentnodeid();
  }

  @Override
  public String component7() {
    return getNamespace();
  }

  @Override
  public String component8() {
    return getName();
  }

  @Override
  public Double component9() {
    return getCpurequest();
  }

  @Override
  public Double component10() {
    return getMemoryrequest();
  }

  @Override
  public String component11() {
    return getWorkloadid();
  }

  @Override
  public OffsetDateTime component12() {
    return getCreatedat();
  }

  @Override
  public OffsetDateTime component13() {
    return getUpdatedat();
  }

  @Override
  public String value1() {
    return getAccountid();
  }

  @Override
  public String value2() {
    return getClusterid();
  }

  @Override
  public String value3() {
    return getInstanceid();
  }

  @Override
  public OffsetDateTime value4() {
    return getStarttime();
  }

  @Override
  public OffsetDateTime value5() {
    return getStoptime();
  }

  @Override
  public String value6() {
    return getParentnodeid();
  }

  @Override
  public String value7() {
    return getNamespace();
  }

  @Override
  public String value8() {
    return getName();
  }

  @Override
  public Double value9() {
    return getCpurequest();
  }

  @Override
  public Double value10() {
    return getMemoryrequest();
  }

  @Override
  public String value11() {
    return getWorkloadid();
  }

  @Override
  public OffsetDateTime value12() {
    return getCreatedat();
  }

  @Override
  public OffsetDateTime value13() {
    return getUpdatedat();
  }

  @Override
  public PodInfoRecord value1(String value) {
    setAccountid(value);
    return this;
  }

  @Override
  public PodInfoRecord value2(String value) {
    setClusterid(value);
    return this;
  }

  @Override
  public PodInfoRecord value3(String value) {
    setInstanceid(value);
    return this;
  }

  @Override
  public PodInfoRecord value4(OffsetDateTime value) {
    setStarttime(value);
    return this;
  }

  @Override
  public PodInfoRecord value5(OffsetDateTime value) {
    setStoptime(value);
    return this;
  }

  @Override
  public PodInfoRecord value6(String value) {
    setParentnodeid(value);
    return this;
  }

  @Override
  public PodInfoRecord value7(String value) {
    setNamespace(value);
    return this;
  }

  @Override
  public PodInfoRecord value8(String value) {
    setName(value);
    return this;
  }

  @Override
  public PodInfoRecord value9(Double value) {
    setCpurequest(value);
    return this;
  }

  @Override
  public PodInfoRecord value10(Double value) {
    setMemoryrequest(value);
    return this;
  }

  @Override
  public PodInfoRecord value11(String value) {
    setWorkloadid(value);
    return this;
  }

  @Override
  public PodInfoRecord value12(OffsetDateTime value) {
    setCreatedat(value);
    return this;
  }

  @Override
  public PodInfoRecord value13(OffsetDateTime value) {
    setUpdatedat(value);
    return this;
  }

  @Override
  public PodInfoRecord values(String value1, String value2, String value3, OffsetDateTime value4, OffsetDateTime value5,
      String value6, String value7, String value8, Double value9, Double value10, String value11,
      OffsetDateTime value12, OffsetDateTime value13) {
    value1(value1);
    value2(value2);
    value3(value3);
    value4(value4);
    value5(value5);
    value6(value6);
    value7(value7);
    value8(value8);
    value9(value9);
    value10(value10);
    value11(value11);
    value12(value12);
    value13(value13);
    return this;
  }

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Create a detached PodInfoRecord
   */
  public PodInfoRecord() {
    super(PodInfo.POD_INFO);
  }

  /**
   * Create a detached, initialised PodInfoRecord
   */
  public PodInfoRecord(String accountid, String clusterid, String instanceid, OffsetDateTime starttime,
      OffsetDateTime stoptime, String parentnodeid, String namespace, String name, Double cpurequest,
      Double memoryrequest, String workloadid, OffsetDateTime createdat, OffsetDateTime updatedat) {
    super(PodInfo.POD_INFO);

    setAccountid(accountid);
    setClusterid(clusterid);
    setInstanceid(instanceid);
    setStarttime(starttime);
    setStoptime(stoptime);
    setParentnodeid(parentnodeid);
    setNamespace(namespace);
    setName(name);
    setCpurequest(cpurequest);
    setMemoryrequest(memoryrequest);
    setWorkloadid(workloadid);
    setCreatedat(createdat);
    setUpdatedat(updatedat);
  }
}
