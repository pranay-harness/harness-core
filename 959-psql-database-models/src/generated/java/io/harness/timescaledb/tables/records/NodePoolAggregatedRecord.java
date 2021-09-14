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

import io.harness.timescaledb.tables.NodePoolAggregated;

import java.time.OffsetDateTime;
import org.jooq.Field;
import org.jooq.Record10;
import org.jooq.Row10;
import org.jooq.impl.TableRecordImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class NodePoolAggregatedRecord extends TableRecordImpl<NodePoolAggregatedRecord> implements Record10<String,
    String, String, Double, Double, Double, Double, OffsetDateTime, OffsetDateTime, OffsetDateTime> {
  private static final long serialVersionUID = 1L;

  /**
   * Setter for <code>public.node_pool_aggregated.name</code>.
   */
  public NodePoolAggregatedRecord setName(String value) {
    set(0, value);
    return this;
  }

  /**
   * Getter for <code>public.node_pool_aggregated.name</code>.
   */
  public String getName() {
    return (String) get(0);
  }

  /**
   * Setter for <code>public.node_pool_aggregated.clusterid</code>.
   */
  public NodePoolAggregatedRecord setClusterid(String value) {
    set(1, value);
    return this;
  }

  /**
   * Getter for <code>public.node_pool_aggregated.clusterid</code>.
   */
  public String getClusterid() {
    return (String) get(1);
  }

  /**
   * Setter for <code>public.node_pool_aggregated.accountid</code>.
   */
  public NodePoolAggregatedRecord setAccountid(String value) {
    set(2, value);
    return this;
  }

  /**
   * Getter for <code>public.node_pool_aggregated.accountid</code>.
   */
  public String getAccountid() {
    return (String) get(2);
  }

  /**
   * Setter for <code>public.node_pool_aggregated.sumcpu</code>.
   */
  public NodePoolAggregatedRecord setSumcpu(Double value) {
    set(3, value);
    return this;
  }

  /**
   * Getter for <code>public.node_pool_aggregated.sumcpu</code>.
   */
  public Double getSumcpu() {
    return (Double) get(3);
  }

  /**
   * Setter for <code>public.node_pool_aggregated.summemory</code>.
   */
  public NodePoolAggregatedRecord setSummemory(Double value) {
    set(4, value);
    return this;
  }

  /**
   * Getter for <code>public.node_pool_aggregated.summemory</code>.
   */
  public Double getSummemory() {
    return (Double) get(4);
  }

  /**
   * Setter for <code>public.node_pool_aggregated.maxcpu</code>.
   */
  public NodePoolAggregatedRecord setMaxcpu(Double value) {
    set(5, value);
    return this;
  }

  /**
   * Getter for <code>public.node_pool_aggregated.maxcpu</code>.
   */
  public Double getMaxcpu() {
    return (Double) get(5);
  }

  /**
   * Setter for <code>public.node_pool_aggregated.maxmemory</code>.
   */
  public NodePoolAggregatedRecord setMaxmemory(Double value) {
    set(6, value);
    return this;
  }

  /**
   * Getter for <code>public.node_pool_aggregated.maxmemory</code>.
   */
  public Double getMaxmemory() {
    return (Double) get(6);
  }

  /**
   * Setter for <code>public.node_pool_aggregated.starttime</code>.
   */
  public NodePoolAggregatedRecord setStarttime(OffsetDateTime value) {
    set(7, value);
    return this;
  }

  /**
   * Getter for <code>public.node_pool_aggregated.starttime</code>.
   */
  public OffsetDateTime getStarttime() {
    return (OffsetDateTime) get(7);
  }

  /**
   * Setter for <code>public.node_pool_aggregated.endtime</code>.
   */
  public NodePoolAggregatedRecord setEndtime(OffsetDateTime value) {
    set(8, value);
    return this;
  }

  /**
   * Getter for <code>public.node_pool_aggregated.endtime</code>.
   */
  public OffsetDateTime getEndtime() {
    return (OffsetDateTime) get(8);
  }

  /**
   * Setter for <code>public.node_pool_aggregated.updatedat</code>.
   */
  public NodePoolAggregatedRecord setUpdatedat(OffsetDateTime value) {
    set(9, value);
    return this;
  }

  /**
   * Getter for <code>public.node_pool_aggregated.updatedat</code>.
   */
  public OffsetDateTime getUpdatedat() {
    return (OffsetDateTime) get(9);
  }

  // -------------------------------------------------------------------------
  // Record10 type implementation
  // -------------------------------------------------------------------------

  @Override
  public Row10<String, String, String, Double, Double, Double, Double, OffsetDateTime, OffsetDateTime, OffsetDateTime>
  fieldsRow() {
    return (Row10) super.fieldsRow();
  }

  @Override
  public Row10<String, String, String, Double, Double, Double, Double, OffsetDateTime, OffsetDateTime, OffsetDateTime>
  valuesRow() {
    return (Row10) super.valuesRow();
  }

  @Override
  public Field<String> field1() {
    return NodePoolAggregated.NODE_POOL_AGGREGATED.NAME;
  }

  @Override
  public Field<String> field2() {
    return NodePoolAggregated.NODE_POOL_AGGREGATED.CLUSTERID;
  }

  @Override
  public Field<String> field3() {
    return NodePoolAggregated.NODE_POOL_AGGREGATED.ACCOUNTID;
  }

  @Override
  public Field<Double> field4() {
    return NodePoolAggregated.NODE_POOL_AGGREGATED.SUMCPU;
  }

  @Override
  public Field<Double> field5() {
    return NodePoolAggregated.NODE_POOL_AGGREGATED.SUMMEMORY;
  }

  @Override
  public Field<Double> field6() {
    return NodePoolAggregated.NODE_POOL_AGGREGATED.MAXCPU;
  }

  @Override
  public Field<Double> field7() {
    return NodePoolAggregated.NODE_POOL_AGGREGATED.MAXMEMORY;
  }

  @Override
  public Field<OffsetDateTime> field8() {
    return NodePoolAggregated.NODE_POOL_AGGREGATED.STARTTIME;
  }

  @Override
  public Field<OffsetDateTime> field9() {
    return NodePoolAggregated.NODE_POOL_AGGREGATED.ENDTIME;
  }

  @Override
  public Field<OffsetDateTime> field10() {
    return NodePoolAggregated.NODE_POOL_AGGREGATED.UPDATEDAT;
  }

  @Override
  public String component1() {
    return getName();
  }

  @Override
  public String component2() {
    return getClusterid();
  }

  @Override
  public String component3() {
    return getAccountid();
  }

  @Override
  public Double component4() {
    return getSumcpu();
  }

  @Override
  public Double component5() {
    return getSummemory();
  }

  @Override
  public Double component6() {
    return getMaxcpu();
  }

  @Override
  public Double component7() {
    return getMaxmemory();
  }

  @Override
  public OffsetDateTime component8() {
    return getStarttime();
  }

  @Override
  public OffsetDateTime component9() {
    return getEndtime();
  }

  @Override
  public OffsetDateTime component10() {
    return getUpdatedat();
  }

  @Override
  public String value1() {
    return getName();
  }

  @Override
  public String value2() {
    return getClusterid();
  }

  @Override
  public String value3() {
    return getAccountid();
  }

  @Override
  public Double value4() {
    return getSumcpu();
  }

  @Override
  public Double value5() {
    return getSummemory();
  }

  @Override
  public Double value6() {
    return getMaxcpu();
  }

  @Override
  public Double value7() {
    return getMaxmemory();
  }

  @Override
  public OffsetDateTime value8() {
    return getStarttime();
  }

  @Override
  public OffsetDateTime value9() {
    return getEndtime();
  }

  @Override
  public OffsetDateTime value10() {
    return getUpdatedat();
  }

  @Override
  public NodePoolAggregatedRecord value1(String value) {
    setName(value);
    return this;
  }

  @Override
  public NodePoolAggregatedRecord value2(String value) {
    setClusterid(value);
    return this;
  }

  @Override
  public NodePoolAggregatedRecord value3(String value) {
    setAccountid(value);
    return this;
  }

  @Override
  public NodePoolAggregatedRecord value4(Double value) {
    setSumcpu(value);
    return this;
  }

  @Override
  public NodePoolAggregatedRecord value5(Double value) {
    setSummemory(value);
    return this;
  }

  @Override
  public NodePoolAggregatedRecord value6(Double value) {
    setMaxcpu(value);
    return this;
  }

  @Override
  public NodePoolAggregatedRecord value7(Double value) {
    setMaxmemory(value);
    return this;
  }

  @Override
  public NodePoolAggregatedRecord value8(OffsetDateTime value) {
    setStarttime(value);
    return this;
  }

  @Override
  public NodePoolAggregatedRecord value9(OffsetDateTime value) {
    setEndtime(value);
    return this;
  }

  @Override
  public NodePoolAggregatedRecord value10(OffsetDateTime value) {
    setUpdatedat(value);
    return this;
  }

  @Override
  public NodePoolAggregatedRecord values(String value1, String value2, String value3, Double value4, Double value5,
      Double value6, Double value7, OffsetDateTime value8, OffsetDateTime value9, OffsetDateTime value10) {
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
    return this;
  }

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Create a detached NodePoolAggregatedRecord
   */
  public NodePoolAggregatedRecord() {
    super(NodePoolAggregated.NODE_POOL_AGGREGATED);
  }

  /**
   * Create a detached, initialised NodePoolAggregatedRecord
   */
  public NodePoolAggregatedRecord(String name, String clusterid, String accountid, Double sumcpu, Double summemory,
      Double maxcpu, Double maxmemory, OffsetDateTime starttime, OffsetDateTime endtime, OffsetDateTime updatedat) {
    super(NodePoolAggregated.NODE_POOL_AGGREGATED);

    setName(name);
    setClusterid(clusterid);
    setAccountid(accountid);
    setSumcpu(sumcpu);
    setSummemory(summemory);
    setMaxcpu(maxcpu);
    setMaxmemory(maxmemory);
    setStarttime(starttime);
    setEndtime(endtime);
    setUpdatedat(updatedat);
  }
}
