/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables.records;

import io.harness.timescaledb.tables.NodeInfo;

import java.time.OffsetDateTime;
import org.jooq.Field;
import org.jooq.Record8;
import org.jooq.Row8;
import org.jooq.impl.TableRecordImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class NodeInfoRecord extends TableRecordImpl<NodeInfoRecord>
    implements Record8<String, String, String, OffsetDateTime, OffsetDateTime, String, OffsetDateTime, OffsetDateTime> {
  private static final long serialVersionUID = 1L;

  /**
   * Setter for <code>public.node_info.accountid</code>.
   */
  public NodeInfoRecord setAccountid(String value) {
    set(0, value);
    return this;
  }

  /**
   * Getter for <code>public.node_info.accountid</code>.
   */
  public String getAccountid() {
    return (String) get(0);
  }

  /**
   * Setter for <code>public.node_info.clusterid</code>.
   */
  public NodeInfoRecord setClusterid(String value) {
    set(1, value);
    return this;
  }

  /**
   * Getter for <code>public.node_info.clusterid</code>.
   */
  public String getClusterid() {
    return (String) get(1);
  }

  /**
   * Setter for <code>public.node_info.instanceid</code>.
   */
  public NodeInfoRecord setInstanceid(String value) {
    set(2, value);
    return this;
  }

  /**
   * Getter for <code>public.node_info.instanceid</code>.
   */
  public String getInstanceid() {
    return (String) get(2);
  }

  /**
   * Setter for <code>public.node_info.starttime</code>.
   */
  public NodeInfoRecord setStarttime(OffsetDateTime value) {
    set(3, value);
    return this;
  }

  /**
   * Getter for <code>public.node_info.starttime</code>.
   */
  public OffsetDateTime getStarttime() {
    return (OffsetDateTime) get(3);
  }

  /**
   * Setter for <code>public.node_info.stoptime</code>.
   */
  public NodeInfoRecord setStoptime(OffsetDateTime value) {
    set(4, value);
    return this;
  }

  /**
   * Getter for <code>public.node_info.stoptime</code>.
   */
  public OffsetDateTime getStoptime() {
    return (OffsetDateTime) get(4);
  }

  /**
   * Setter for <code>public.node_info.nodepoolname</code>.
   */
  public NodeInfoRecord setNodepoolname(String value) {
    set(5, value);
    return this;
  }

  /**
   * Getter for <code>public.node_info.nodepoolname</code>.
   */
  public String getNodepoolname() {
    return (String) get(5);
  }

  /**
   * Setter for <code>public.node_info.createdat</code>.
   */
  public NodeInfoRecord setCreatedat(OffsetDateTime value) {
    set(6, value);
    return this;
  }

  /**
   * Getter for <code>public.node_info.createdat</code>.
   */
  public OffsetDateTime getCreatedat() {
    return (OffsetDateTime) get(6);
  }

  /**
   * Setter for <code>public.node_info.updatedat</code>.
   */
  public NodeInfoRecord setUpdatedat(OffsetDateTime value) {
    set(7, value);
    return this;
  }

  /**
   * Getter for <code>public.node_info.updatedat</code>.
   */
  public OffsetDateTime getUpdatedat() {
    return (OffsetDateTime) get(7);
  }

  // -------------------------------------------------------------------------
  // Record8 type implementation
  // -------------------------------------------------------------------------

  @Override
  public Row8<String, String, String, OffsetDateTime, OffsetDateTime, String, OffsetDateTime, OffsetDateTime>
  fieldsRow() {
    return (Row8) super.fieldsRow();
  }

  @Override
  public Row8<String, String, String, OffsetDateTime, OffsetDateTime, String, OffsetDateTime, OffsetDateTime>
  valuesRow() {
    return (Row8) super.valuesRow();
  }

  @Override
  public Field<String> field1() {
    return NodeInfo.NODE_INFO.ACCOUNTID;
  }

  @Override
  public Field<String> field2() {
    return NodeInfo.NODE_INFO.CLUSTERID;
  }

  @Override
  public Field<String> field3() {
    return NodeInfo.NODE_INFO.INSTANCEID;
  }

  @Override
  public Field<OffsetDateTime> field4() {
    return NodeInfo.NODE_INFO.STARTTIME;
  }

  @Override
  public Field<OffsetDateTime> field5() {
    return NodeInfo.NODE_INFO.STOPTIME;
  }

  @Override
  public Field<String> field6() {
    return NodeInfo.NODE_INFO.NODEPOOLNAME;
  }

  @Override
  public Field<OffsetDateTime> field7() {
    return NodeInfo.NODE_INFO.CREATEDAT;
  }

  @Override
  public Field<OffsetDateTime> field8() {
    return NodeInfo.NODE_INFO.UPDATEDAT;
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
    return getNodepoolname();
  }

  @Override
  public OffsetDateTime component7() {
    return getCreatedat();
  }

  @Override
  public OffsetDateTime component8() {
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
    return getNodepoolname();
  }

  @Override
  public OffsetDateTime value7() {
    return getCreatedat();
  }

  @Override
  public OffsetDateTime value8() {
    return getUpdatedat();
  }

  @Override
  public NodeInfoRecord value1(String value) {
    setAccountid(value);
    return this;
  }

  @Override
  public NodeInfoRecord value2(String value) {
    setClusterid(value);
    return this;
  }

  @Override
  public NodeInfoRecord value3(String value) {
    setInstanceid(value);
    return this;
  }

  @Override
  public NodeInfoRecord value4(OffsetDateTime value) {
    setStarttime(value);
    return this;
  }

  @Override
  public NodeInfoRecord value5(OffsetDateTime value) {
    setStoptime(value);
    return this;
  }

  @Override
  public NodeInfoRecord value6(String value) {
    setNodepoolname(value);
    return this;
  }

  @Override
  public NodeInfoRecord value7(OffsetDateTime value) {
    setCreatedat(value);
    return this;
  }

  @Override
  public NodeInfoRecord value8(OffsetDateTime value) {
    setUpdatedat(value);
    return this;
  }

  @Override
  public NodeInfoRecord values(String value1, String value2, String value3, OffsetDateTime value4,
      OffsetDateTime value5, String value6, OffsetDateTime value7, OffsetDateTime value8) {
    value1(value1);
    value2(value2);
    value3(value3);
    value4(value4);
    value5(value5);
    value6(value6);
    value7(value7);
    value8(value8);
    return this;
  }

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Create a detached NodeInfoRecord
   */
  public NodeInfoRecord() {
    super(NodeInfo.NODE_INFO);
  }

  /**
   * Create a detached, initialised NodeInfoRecord
   */
  public NodeInfoRecord(String accountid, String clusterid, String instanceid, OffsetDateTime starttime,
      OffsetDateTime stoptime, String nodepoolname, OffsetDateTime createdat, OffsetDateTime updatedat) {
    super(NodeInfo.NODE_INFO);

    setAccountid(accountid);
    setClusterid(clusterid);
    setInstanceid(instanceid);
    setStarttime(starttime);
    setStoptime(stoptime);
    setNodepoolname(nodepoolname);
    setCreatedat(createdat);
    setUpdatedat(updatedat);
  }
}
