/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables;

import io.harness.timescaledb.Indexes;
import io.harness.timescaledb.Public;
import io.harness.timescaledb.tables.records.NgInstanceStatsRecord;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row9;
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
public class NgInstanceStats extends TableImpl<NgInstanceStatsRecord> {
  private static final long serialVersionUID = 1L;

  /**
   * The reference instance of <code>public.ng_instance_stats</code>
   */
  public static final NgInstanceStats NG_INSTANCE_STATS = new NgInstanceStats();

  /**
   * The class holding records for this type
   */
  @Override
  public Class<NgInstanceStatsRecord> getRecordType() {
    return NgInstanceStatsRecord.class;
  }

  /**
   * The column <code>public.ng_instance_stats.reportedat</code>.
   */
  public final TableField<NgInstanceStatsRecord, OffsetDateTime> REPORTEDAT =
      createField(DSL.name("reportedat"), SQLDataType.TIMESTAMPWITHTIMEZONE(6).nullable(false), this, "");

  /**
   * The column <code>public.ng_instance_stats.accountid</code>.
   */
  public final TableField<NgInstanceStatsRecord, String> ACCOUNTID =
      createField(DSL.name("accountid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.ng_instance_stats.orgid</code>.
   */
  public final TableField<NgInstanceStatsRecord, String> ORGID =
      createField(DSL.name("orgid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.ng_instance_stats.projectid</code>.
   */
  public final TableField<NgInstanceStatsRecord, String> PROJECTID =
      createField(DSL.name("projectid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.ng_instance_stats.serviceid</code>.
   */
  public final TableField<NgInstanceStatsRecord, String> SERVICEID =
      createField(DSL.name("serviceid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.ng_instance_stats.envid</code>.
   */
  public final TableField<NgInstanceStatsRecord, String> ENVID =
      createField(DSL.name("envid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.ng_instance_stats.cloudproviderid</code>.
   */
  public final TableField<NgInstanceStatsRecord, String> CLOUDPROVIDERID =
      createField(DSL.name("cloudproviderid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.ng_instance_stats.instancetype</code>.
   */
  public final TableField<NgInstanceStatsRecord, String> INSTANCETYPE =
      createField(DSL.name("instancetype"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.ng_instance_stats.instancecount</code>.
   */
  public final TableField<NgInstanceStatsRecord, Integer> INSTANCECOUNT =
      createField(DSL.name("instancecount"), SQLDataType.INTEGER, this, "");

  private NgInstanceStats(Name alias, Table<NgInstanceStatsRecord> aliased) {
    this(alias, aliased, null);
  }

  private NgInstanceStats(Name alias, Table<NgInstanceStatsRecord> aliased, Field<?>[] parameters) {
    super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
  }

  /**
   * Create an aliased <code>public.ng_instance_stats</code> table reference
   */
  public NgInstanceStats(String alias) {
    this(DSL.name(alias), NG_INSTANCE_STATS);
  }

  /**
   * Create an aliased <code>public.ng_instance_stats</code> table reference
   */
  public NgInstanceStats(Name alias) {
    this(alias, NG_INSTANCE_STATS);
  }

  /**
   * Create a <code>public.ng_instance_stats</code> table reference
   */
  public NgInstanceStats() {
    this(DSL.name("ng_instance_stats"), null);
  }

  public <O extends Record> NgInstanceStats(Table<O> child, ForeignKey<O, NgInstanceStatsRecord> key) {
    super(child, key, NG_INSTANCE_STATS);
  }

  @Override
  public Schema getSchema() {
    return Public.PUBLIC;
  }

  @Override
  public List<Index> getIndexes() {
    return Arrays.<Index>asList(Indexes.NG_INSTANCE_STATS_ACCOUNTID_INDEX,
        Indexes.NG_INSTANCE_STATS_CLOUDPROVIDERID_INDEX, Indexes.NG_INSTANCE_STATS_ENVID_INDEX,
        Indexes.NG_INSTANCE_STATS_INSTANCECOUNT_INDEX, Indexes.NG_INSTANCE_STATS_ORGID_INDEX,
        Indexes.NG_INSTANCE_STATS_PROJECTID_INDEX, Indexes.NG_INSTANCE_STATS_REPORTEDAT_IDX,
        Indexes.NG_INSTANCE_STATS_SERVICEID_INDEX);
  }

  @Override
  public NgInstanceStats as(String alias) {
    return new NgInstanceStats(DSL.name(alias), this);
  }

  @Override
  public NgInstanceStats as(Name alias) {
    return new NgInstanceStats(alias, this);
  }

  /**
   * Rename this table
   */
  @Override
  public NgInstanceStats rename(String name) {
    return new NgInstanceStats(DSL.name(name), null);
  }

  /**
   * Rename this table
   */
  @Override
  public NgInstanceStats rename(Name name) {
    return new NgInstanceStats(name, null);
  }

  // -------------------------------------------------------------------------
  // Row9 type methods
  // -------------------------------------------------------------------------

  @Override
  public Row9<OffsetDateTime, String, String, String, String, String, String, String, Integer> fieldsRow() {
    return (Row9) super.fieldsRow();
  }
}
