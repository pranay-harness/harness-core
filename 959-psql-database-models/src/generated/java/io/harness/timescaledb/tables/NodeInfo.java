/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables;

import io.harness.timescaledb.Indexes;
import io.harness.timescaledb.Keys;
import io.harness.timescaledb.Public;
import io.harness.timescaledb.tables.records.NodeInfoRecord;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row8;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class NodeInfo extends TableImpl<NodeInfoRecord> {
  private static final long serialVersionUID = 1L;

  /**
   * The reference instance of <code>public.node_info</code>
   */
  public static final NodeInfo NODE_INFO = new NodeInfo();

  /**
   * The class holding records for this type
   */
  @Override
  public Class<NodeInfoRecord> getRecordType() {
    return NodeInfoRecord.class;
  }

  /**
   * The column <code>public.node_info.accountid</code>.
   */
  public final TableField<NodeInfoRecord, String> ACCOUNTID =
      createField(DSL.name("accountid"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.node_info.clusterid</code>.
   */
  public final TableField<NodeInfoRecord, String> CLUSTERID =
      createField(DSL.name("clusterid"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.node_info.instanceid</code>.
   */
  public final TableField<NodeInfoRecord, String> INSTANCEID =
      createField(DSL.name("instanceid"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.node_info.starttime</code>.
   */
  public final TableField<NodeInfoRecord, OffsetDateTime> STARTTIME =
      createField(DSL.name("starttime"), SQLDataType.TIMESTAMPWITHTIMEZONE(6).nullable(false), this, "");

  /**
   * The column <code>public.node_info.stoptime</code>.
   */
  public final TableField<NodeInfoRecord, OffsetDateTime> STOPTIME =
      createField(DSL.name("stoptime"), SQLDataType.TIMESTAMPWITHTIMEZONE(6), this, "");

  /**
   * The column <code>public.node_info.nodepoolname</code>.
   */
  public final TableField<NodeInfoRecord, String> NODEPOOLNAME =
      createField(DSL.name("nodepoolname"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.node_info.createdat</code>.
   */
  public final TableField<NodeInfoRecord, OffsetDateTime> CREATEDAT = createField(DSL.name("createdat"),
      SQLDataType.TIMESTAMPWITHTIMEZONE(6).defaultValue(DSL.field("now()", SQLDataType.TIMESTAMPWITHTIMEZONE)), this,
      "");

  /**
   * The column <code>public.node_info.updatedat</code>.
   */
  public final TableField<NodeInfoRecord, OffsetDateTime> UPDATEDAT = createField(DSL.name("updatedat"),
      SQLDataType.TIMESTAMPWITHTIMEZONE(6).defaultValue(DSL.field("now()", SQLDataType.TIMESTAMPWITHTIMEZONE)), this,
      "");

  private NodeInfo(Name alias, Table<NodeInfoRecord> aliased) {
    this(alias, aliased, null);
  }

  private NodeInfo(Name alias, Table<NodeInfoRecord> aliased, Field<?>[] parameters) {
    super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
  }

  /**
   * Create an aliased <code>public.node_info</code> table reference
   */
  public NodeInfo(String alias) {
    this(DSL.name(alias), NODE_INFO);
  }

  /**
   * Create an aliased <code>public.node_info</code> table reference
   */
  public NodeInfo(Name alias) {
    this(alias, NODE_INFO);
  }

  /**
   * Create a <code>public.node_info</code> table reference
   */
  public NodeInfo() {
    this(DSL.name("node_info"), null);
  }

  public <O extends Record> NodeInfo(Table<O> child, ForeignKey<O, NodeInfoRecord> key) {
    super(child, key, NODE_INFO);
  }

  @Override
  public Schema getSchema() {
    return Public.PUBLIC;
  }

  @Override
  public List<Index> getIndexes() {
    return Arrays.<Index>asList(Indexes.NODE_INFO_ACCID_CLUSTERID_POOLNAME);
  }

  @Override
  public List<UniqueKey<NodeInfoRecord>> getKeys() {
    return Arrays.<UniqueKey<NodeInfoRecord>>asList(Keys.NODE_INFO_UNIQUE_RECORD_INDEX);
  }

  @Override
  public NodeInfo as(String alias) {
    return new NodeInfo(DSL.name(alias), this);
  }

  @Override
  public NodeInfo as(Name alias) {
    return new NodeInfo(alias, this);
  }

  /**
   * Rename this table
   */
  @Override
  public NodeInfo rename(String name) {
    return new NodeInfo(DSL.name(name), null);
  }

  /**
   * Rename this table
   */
  @Override
  public NodeInfo rename(Name name) {
    return new NodeInfo(name, null);
  }

  // -------------------------------------------------------------------------
  // Row8 type methods
  // -------------------------------------------------------------------------

  @Override
  public Row8<String, String, String, OffsetDateTime, OffsetDateTime, String, OffsetDateTime, OffsetDateTime>
  fieldsRow() {
    return (Row8) super.fieldsRow();
  }
}
