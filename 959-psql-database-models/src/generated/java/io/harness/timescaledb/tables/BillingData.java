/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables;

import io.harness.timescaledb.Indexes;
import io.harness.timescaledb.Public;
import io.harness.timescaledb.tables.records.BillingDataRecord;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class BillingData extends TableImpl<BillingDataRecord> {
  private static final long serialVersionUID = 1L;

  /**
   * The reference instance of <code>public.billing_data</code>
   */
  public static final BillingData BILLING_DATA = new BillingData();

  /**
   * The class holding records for this type
   */
  @Override
  public Class<BillingDataRecord> getRecordType() {
    return BillingDataRecord.class;
  }

  /**
   * The column <code>public.billing_data.starttime</code>.
   */
  public final TableField<BillingDataRecord, OffsetDateTime> STARTTIME =
      createField(DSL.name("starttime"), SQLDataType.TIMESTAMPWITHTIMEZONE(6).nullable(false), this, "");

  /**
   * The column <code>public.billing_data.endtime</code>.
   */
  public final TableField<BillingDataRecord, OffsetDateTime> ENDTIME =
      createField(DSL.name("endtime"), SQLDataType.TIMESTAMPWITHTIMEZONE(6).nullable(false), this, "");

  /**
   * The column <code>public.billing_data.accountid</code>.
   */
  public final TableField<BillingDataRecord, String> ACCOUNTID =
      createField(DSL.name("accountid"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.billing_data.settingid</code>.
   */
  public final TableField<BillingDataRecord, String> SETTINGID =
      createField(DSL.name("settingid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.instanceid</code>.
   */
  public final TableField<BillingDataRecord, String> INSTANCEID =
      createField(DSL.name("instanceid"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.billing_data.instancetype</code>.
   */
  public final TableField<BillingDataRecord, String> INSTANCETYPE =
      createField(DSL.name("instancetype"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.billing_data.billingaccountid</code>.
   */
  public final TableField<BillingDataRecord, String> BILLINGACCOUNTID =
      createField(DSL.name("billingaccountid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.clusterid</code>.
   */
  public final TableField<BillingDataRecord, String> CLUSTERID =
      createField(DSL.name("clusterid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.clustername</code>.
   */
  public final TableField<BillingDataRecord, String> CLUSTERNAME =
      createField(DSL.name("clustername"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.appid</code>.
   */
  public final TableField<BillingDataRecord, String> APPID = createField(DSL.name("appid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.serviceid</code>.
   */
  public final TableField<BillingDataRecord, String> SERVICEID =
      createField(DSL.name("serviceid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.envid</code>.
   */
  public final TableField<BillingDataRecord, String> ENVID = createField(DSL.name("envid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.cloudproviderid</code>.
   */
  public final TableField<BillingDataRecord, String> CLOUDPROVIDERID =
      createField(DSL.name("cloudproviderid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.parentinstanceid</code>.
   */
  public final TableField<BillingDataRecord, String> PARENTINSTANCEID =
      createField(DSL.name("parentinstanceid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.region</code>.
   */
  public final TableField<BillingDataRecord, String> REGION =
      createField(DSL.name("region"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.launchtype</code>.
   */
  public final TableField<BillingDataRecord, String> LAUNCHTYPE =
      createField(DSL.name("launchtype"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.clustertype</code>.
   */
  public final TableField<BillingDataRecord, String> CLUSTERTYPE =
      createField(DSL.name("clustertype"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.workloadname</code>.
   */
  public final TableField<BillingDataRecord, String> WORKLOADNAME =
      createField(DSL.name("workloadname"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.workloadtype</code>.
   */
  public final TableField<BillingDataRecord, String> WORKLOADTYPE =
      createField(DSL.name("workloadtype"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.namespace</code>.
   */
  public final TableField<BillingDataRecord, String> NAMESPACE =
      createField(DSL.name("namespace"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.cloudservicename</code>.
   */
  public final TableField<BillingDataRecord, String> CLOUDSERVICENAME =
      createField(DSL.name("cloudservicename"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.taskid</code>.
   */
  public final TableField<BillingDataRecord, String> TASKID =
      createField(DSL.name("taskid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.cloudprovider</code>.
   */
  public final TableField<BillingDataRecord, String> CLOUDPROVIDER =
      createField(DSL.name("cloudprovider"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.billingamount</code>.
   */
  public final TableField<BillingDataRecord, Double> BILLINGAMOUNT =
      createField(DSL.name("billingamount"), SQLDataType.DOUBLE.nullable(false), this, "");

  /**
   * The column <code>public.billing_data.cpubillingamount</code>.
   */
  public final TableField<BillingDataRecord, Double> CPUBILLINGAMOUNT =
      createField(DSL.name("cpubillingamount"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.memorybillingamount</code>.
   */
  public final TableField<BillingDataRecord, Double> MEMORYBILLINGAMOUNT =
      createField(DSL.name("memorybillingamount"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.idlecost</code>.
   */
  public final TableField<BillingDataRecord, Double> IDLECOST =
      createField(DSL.name("idlecost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.cpuidlecost</code>.
   */
  public final TableField<BillingDataRecord, Double> CPUIDLECOST =
      createField(DSL.name("cpuidlecost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.memoryidlecost</code>.
   */
  public final TableField<BillingDataRecord, Double> MEMORYIDLECOST =
      createField(DSL.name("memoryidlecost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.usagedurationseconds</code>.
   */
  public final TableField<BillingDataRecord, Double> USAGEDURATIONSECONDS =
      createField(DSL.name("usagedurationseconds"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.cpuunitseconds</code>.
   */
  public final TableField<BillingDataRecord, Double> CPUUNITSECONDS =
      createField(DSL.name("cpuunitseconds"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.memorymbseconds</code>.
   */
  public final TableField<BillingDataRecord, Double> MEMORYMBSECONDS =
      createField(DSL.name("memorymbseconds"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.maxcpuutilization</code>.
   */
  public final TableField<BillingDataRecord, Double> MAXCPUUTILIZATION =
      createField(DSL.name("maxcpuutilization"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.maxmemoryutilization</code>.
   */
  public final TableField<BillingDataRecord, Double> MAXMEMORYUTILIZATION =
      createField(DSL.name("maxmemoryutilization"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.avgcpuutilization</code>.
   */
  public final TableField<BillingDataRecord, Double> AVGCPUUTILIZATION =
      createField(DSL.name("avgcpuutilization"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.avgmemoryutilization</code>.
   */
  public final TableField<BillingDataRecord, Double> AVGMEMORYUTILIZATION =
      createField(DSL.name("avgmemoryutilization"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.systemcost</code>.
   */
  public final TableField<BillingDataRecord, Double> SYSTEMCOST =
      createField(DSL.name("systemcost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.cpusystemcost</code>.
   */
  public final TableField<BillingDataRecord, Double> CPUSYSTEMCOST =
      createField(DSL.name("cpusystemcost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.memorysystemcost</code>.
   */
  public final TableField<BillingDataRecord, Double> MEMORYSYSTEMCOST =
      createField(DSL.name("memorysystemcost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.actualidlecost</code>.
   */
  public final TableField<BillingDataRecord, Double> ACTUALIDLECOST =
      createField(DSL.name("actualidlecost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.cpuactualidlecost</code>.
   */
  public final TableField<BillingDataRecord, Double> CPUACTUALIDLECOST =
      createField(DSL.name("cpuactualidlecost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.memoryactualidlecost</code>.
   */
  public final TableField<BillingDataRecord, Double> MEMORYACTUALIDLECOST =
      createField(DSL.name("memoryactualidlecost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.unallocatedcost</code>.
   */
  public final TableField<BillingDataRecord, Double> UNALLOCATEDCOST =
      createField(DSL.name("unallocatedcost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.cpuunallocatedcost</code>.
   */
  public final TableField<BillingDataRecord, Double> CPUUNALLOCATEDCOST =
      createField(DSL.name("cpuunallocatedcost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.memoryunallocatedcost</code>.
   */
  public final TableField<BillingDataRecord, Double> MEMORYUNALLOCATEDCOST =
      createField(DSL.name("memoryunallocatedcost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.instancename</code>.
   */
  public final TableField<BillingDataRecord, String> INSTANCENAME =
      createField(DSL.name("instancename"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.cpurequest</code>.
   */
  public final TableField<BillingDataRecord, Double> CPUREQUEST =
      createField(DSL.name("cpurequest"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.memoryrequest</code>.
   */
  public final TableField<BillingDataRecord, Double> MEMORYREQUEST =
      createField(DSL.name("memoryrequest"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.cpulimit</code>.
   */
  public final TableField<BillingDataRecord, Double> CPULIMIT =
      createField(DSL.name("cpulimit"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.memorylimit</code>.
   */
  public final TableField<BillingDataRecord, Double> MEMORYLIMIT =
      createField(DSL.name("memorylimit"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.maxcpuutilizationvalue</code>.
   */
  public final TableField<BillingDataRecord, Double> MAXCPUUTILIZATIONVALUE =
      createField(DSL.name("maxcpuutilizationvalue"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.maxmemoryutilizationvalue</code>.
   */
  public final TableField<BillingDataRecord, Double> MAXMEMORYUTILIZATIONVALUE =
      createField(DSL.name("maxmemoryutilizationvalue"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.avgcpuutilizationvalue</code>.
   */
  public final TableField<BillingDataRecord, Double> AVGCPUUTILIZATIONVALUE =
      createField(DSL.name("avgcpuutilizationvalue"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.avgmemoryutilizationvalue</code>.
   */
  public final TableField<BillingDataRecord, Double> AVGMEMORYUTILIZATIONVALUE =
      createField(DSL.name("avgmemoryutilizationvalue"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.networkcost</code>.
   */
  public final TableField<BillingDataRecord, Double> NETWORKCOST =
      createField(DSL.name("networkcost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.pricingsource</code>.
   */
  public final TableField<BillingDataRecord, String> PRICINGSOURCE =
      createField(DSL.name("pricingsource"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.billing_data.storageactualidlecost</code>.
   */
  public final TableField<BillingDataRecord, Double> STORAGEACTUALIDLECOST =
      createField(DSL.name("storageactualidlecost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.storageunallocatedcost</code>.
   */
  public final TableField<BillingDataRecord, Double> STORAGEUNALLOCATEDCOST =
      createField(DSL.name("storageunallocatedcost"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.storageutilizationvalue</code>.
   */
  public final TableField<BillingDataRecord, Double> STORAGEUTILIZATIONVALUE =
      createField(DSL.name("storageutilizationvalue"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.storagerequest</code>.
   */
  public final TableField<BillingDataRecord, Double> STORAGEREQUEST =
      createField(DSL.name("storagerequest"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.storagembseconds</code>.
   */
  public final TableField<BillingDataRecord, Double> STORAGEMBSECONDS =
      createField(DSL.name("storagembseconds"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.billing_data.storagecost</code>.
   */
  public final TableField<BillingDataRecord, Double> STORAGECOST =
      createField(DSL.name("storagecost"), SQLDataType.DOUBLE, this, "");

  private BillingData(Name alias, Table<BillingDataRecord> aliased) {
    this(alias, aliased, null);
  }

  private BillingData(Name alias, Table<BillingDataRecord> aliased, Field<?>[] parameters) {
    super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
  }

  /**
   * Create an aliased <code>public.billing_data</code> table reference
   */
  public BillingData(String alias) {
    this(DSL.name(alias), BILLING_DATA);
  }

  /**
   * Create an aliased <code>public.billing_data</code> table reference
   */
  public BillingData(Name alias) {
    this(alias, BILLING_DATA);
  }

  /**
   * Create a <code>public.billing_data</code> table reference
   */
  public BillingData() {
    this(DSL.name("billing_data"), null);
  }

  public <O extends Record> BillingData(Table<O> child, ForeignKey<O, BillingDataRecord> key) {
    super(child, key, BILLING_DATA);
  }

  @Override
  public Schema getSchema() {
    return Public.PUBLIC;
  }

  @Override
  public List<Index> getIndexes() {
    return Arrays.<Index>asList(Indexes.BILLING_DATA_ACCOUNTID_INDEX, Indexes.BILLING_DATA_APPID_COMPOSITE_INDEX,
        Indexes.BILLING_DATA_CLOUDPROVIDERID_COMPOSITE_INDEX, Indexes.BILLING_DATA_CLOUDSERVICENAME_COMPOSITE_INDEX,
        Indexes.BILLING_DATA_CLUSTERID_COMPOSITE_INDEX, Indexes.BILLING_DATA_LAUNCHTYPE_COMPOSITE_INDEX,
        Indexes.BILLING_DATA_NAMESPACE_COMPOSITE_INDEX, Indexes.BILLING_DATA_NAMESPACE_WITHOUT_CLUSTER_INDEX,
        Indexes.BILLING_DATA_STARTTIME_IDX, Indexes.BILLING_DATA_TASKID_COMPOSITE_INDEX,
        Indexes.BILLING_DATA_UNIQUE_INDEX, Indexes.BILLING_DATA_WORKLOADNAME_COMPOSITE_INDEX,
        Indexes.BILLING_DATA_WORKLOADNAME_WITHOUT_CLUSTER_INDEX);
  }

  @Override
  public BillingData as(String alias) {
    return new BillingData(DSL.name(alias), this);
  }

  @Override
  public BillingData as(Name alias) {
    return new BillingData(alias, this);
  }

  /**
   * Rename this table
   */
  @Override
  public BillingData rename(String name) {
    return new BillingData(DSL.name(name), null);
  }

  /**
   * Rename this table
   */
  @Override
  public BillingData rename(Name name) {
    return new BillingData(name, null);
  }
}
