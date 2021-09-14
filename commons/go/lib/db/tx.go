// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package db

import (
	"context"
	"database/sql"
	"time"

	uuid "github.com/satori/go.uuid"
	"go.uber.org/zap"
)

// Tx wraps a *sql.Tx with logging
type Tx struct {
	start time.Time
	id    uuid.UUID
	base  *sql.Tx
	log   *zap.SugaredLogger
	db    *DB
}

var _ Querier = &Tx{}

//Exec runs the given sql statement on the base transaction, logs details about the call and returns the result
func (tx *Tx) Exec(query string, args ...interface{}) (sql.Result, error) {
	start := time.Now()
	res, err := tx.base.Exec(query, args...)
	logQuery(tx.log, start, query, args, err)
	return res, err
}

//Query runs the given sql statement on the base transaction, logs details about the call and returns the result
func (tx *Tx) Query(query string, args ...interface{}) (*sql.Rows, error) {
	start := time.Now()
	res, err := tx.base.Query(query, args...)
	logQuery(tx.log, start, query, args, err)
	return res, err
}

//QueryRow runs the given sql statement on the base transaction, logs details about the call and returns the result
func (tx *Tx) QueryRow(query string, args ...interface{}) *sql.Row {
	start := time.Now()
	row := tx.base.QueryRow(query, args...)
	logQuery(tx.log, start, query, args, nil)
	return row
}

// Prepare prepares a statement with the underlying base transaction, logs, and then returns a statement
func (tx *Tx) Prepare(query string) (*Stmt, error) {
	start := time.Now()
	stmt, err := tx.base.Prepare(query)
	if err != nil {
		return nil, err
	}
	tx.log.Infow("sql prepare", "sql.query", collapseSpaces(query), "sql.hash", hash(query), "query_time_ms", ms(time.Since(start)))
	return newStmt(stmt, query, tx.db, tx.log), nil
}

//ExecContext runs the given sql statement using the given context on the base transaction,
//logs details about the call and returns the result
func (tx *Tx) ExecContext(ctx context.Context, query string, args ...interface{}) (sql.Result, error) {
	start := time.Now()
	deferFunc := startNewSpanIfContextHasSpan(ctx, "tx.base.ExecContext", tx.db.ci.Application, tx.db.ci.DBName, tx.db.ci.Engine, query)
	defer deferFunc()
	res, err := tx.base.ExecContext(ctx, query, args...)
	logQuery(tx.log, start, query, args, err)
	return res, err
}

// QueryContext runs the given sql statement using the given context on the base transaction,
// logs details about the call and returns the result
func (tx *Tx) QueryContext(ctx context.Context, query string, args ...interface{}) (*sql.Rows, error) {
	start := time.Now()
	deferFunc := startNewSpanIfContextHasSpan(ctx, "tx.base.QueryContext", tx.db.ci.Application, tx.db.ci.DBName, tx.db.ci.Engine, query)
	defer deferFunc()
	rows, err := tx.base.QueryContext(ctx, query, args...)
	logQuery(tx.log, start, query, args, err)
	return rows, err
}

// QueryRowContext runs the given sql statement using the given context on the base transaction,
// logs details about the call and returns the result
func (tx *Tx) QueryRowContext(ctx context.Context, query string, args ...interface{}) *sql.Row {
	start := time.Now()
	deferFunc := startNewSpanIfContextHasSpan(ctx, "tx.base.QueryRowContext", tx.db.ci.Application, tx.db.ci.DBName, tx.db.ci.Engine, query)
	defer deferFunc()
	row := tx.base.QueryRowContext(ctx, query, args...)
	logQuery(tx.log, start, query, args, nil)
	return row
}

// PrepareContext prepares a statement with the underlying context on the base transaction, logs, and then returns a statement
func (tx *Tx) PrepareContext(ctx context.Context, query string) (*Stmt, error) {
	start := time.Now()
	deferFunc := startNewSpanIfContextHasSpan(ctx, "tx.base.PrepareContext", tx.db.ci.Application, tx.db.ci.DBName, tx.db.ci.Engine, query)
	defer deferFunc()
	stmt, err := tx.base.PrepareContext(ctx, query)
	if err != nil {
		return nil, err
	}
	tx.log.Infow("sql prepare", "sql.query", collapseSpaces(query), "sql.hash", hash(query), "query_time_ms", ms(time.Since(start)))
	return newStmt(stmt, query, tx.db, tx.log), nil
}

func newTx(base *sql.Tx, db *DB, log *zap.SugaredLogger) *Tx {
	tx := &Tx{
		id:    uuid.NewV4(),
		start: time.Now(),
		base:  base,
		db:    db,
	}
	tx.log = log.With("sql.tx_id", tx.id)
	return tx
}

//Commit logs and runs commit on the base transaction
func (tx *Tx) Commit() error {
	tx.log.Infow("sql transaction commit", "tx_dur_ms", ms(time.Since(tx.start)))
	return tx.base.Commit()
}

//Rollback logs and runs rollback on the base transaction
func (tx *Tx) Rollback() error {
	tx.log.Infow("sql transaction rollback", "tx_dur_ms", ms(time.Since(tx.start)))
	return tx.base.Rollback()
}
