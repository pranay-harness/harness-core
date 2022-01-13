// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package db

import (
	"context"
	"database/sql"
	"time"

	opentracing "github.com/opentracing/opentracing-go"
	"github.com/opentracing/opentracing-go/ext"
	logger "github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
)

// A Querier can be used to issue queries, like a DB a Tx, or a Stmt
type Querier interface {
	Exec(query string, args ...interface{}) (sql.Result, error)
	Query(query string, args ...interface{}) (*sql.Rows, error)
	QueryRow(query string, args ...interface{}) *sql.Row
	Prepare(query string) (*Stmt, error)
	ExecContext(ctx context.Context, query string, args ...interface{}) (sql.Result, error)
	QueryContext(ctx context.Context, query string, args ...interface{}) (*sql.Rows, error)
	QueryRowContext(ctx context.Context, query string, args ...interface{}) *sql.Row
	PrepareContext(ctx context.Context, query string) (*Stmt, error)
}

var _ Querier = &DB{}

// DB wraps sql.DB with logging.
type DB struct {
	ci   ConnectionInfo
	conn *sql.DB
	log  *zap.SugaredLogger
}

// Config is used to configure the connection limits
type Config struct {
	MaxOpenConnections    int
	MaxIdleConnections    int
	ConnectionMaxLifetime time.Duration
}

// NewDB returns a DB
func NewDB(ci *ConnectionInfo, log *zap.SugaredLogger) (*DB, error) {
	config := Config{
		// Decreases the max open connections from the default of unlimited
		MaxOpenConnections: 30,

		// Increases the max idle connections from the default of 2
		MaxIdleConnections: 30,

		// Decrease the maximum lifetime of idle connections from default of forever
		ConnectionMaxLifetime: time.Minute * 2,
	}

	return NewDBWithConfig(ci, log, config)
}

// NewDBWithConfig returns a DB with a given Config
func NewDBWithConfig(ci *ConnectionInfo, log *zap.SugaredLogger, config Config) (*DB, error) {

	conn, err := ci.getDBConnection()
	if err != nil {
		return nil, err
	}
	conn.SetMaxOpenConns(config.MaxOpenConnections)
	conn.SetMaxIdleConns(config.MaxIdleConnections)
	conn.SetConnMaxLifetime(config.ConnectionMaxLifetime)

	db := &DB{
		ci:   *ci, // To be used in span tagging
		conn: conn,
		log:  log,
	}

	return db, nil
}

//Exec runs the given sql statement using db.Exec, logs details about the call and returns the result
func (db *DB) Exec(query string, args ...interface{}) (sql.Result, error) {
	start := time.Now()
	res, err := db.conn.Exec(query, args...)
	logQuery(db.log, start, query, args, err)
	return res, err
}

//Query runs the given sql statement using db.Query, logs details about the call and returns the result
func (db *DB) Query(query string, args ...interface{}) (*sql.Rows, error) {
	start := time.Now()
	res, err := db.conn.Query(query, args...)
	logQuery(db.log, start, query, args, err)
	return res, err
}

//QueryRow runs the given sql statement using db.QueryRow, logs details about the call and returns the result
func (db *DB) QueryRow(query string, args ...interface{}) *sql.Row {
	start := time.Now()
	row := db.conn.QueryRow(query, args...)
	logQuery(db.log, start, query, args, nil)
	return row
}

// Prepare prepares a statement with the underlying database connection, logs, and then returns a statement
func (db *DB) Prepare(query string) (*Stmt, error) {
	start := time.Now()
	stmt, err := db.conn.Prepare(query)
	if err != nil {
		return nil, err
	}
	db.log.Infow("sql prepare", "sql.query", collapseSpaces(query), "sql.hash", hash(query), "query_time_ms", ms(time.Since(start)))
	return newStmt(stmt, query, db, db.log), nil
}

// ExecContext runs the given sql statement using the given context, db.Exec, logs details about the call and returns the result
func (db *DB) ExecContext(ctx context.Context, query string, args ...interface{}) (sql.Result, error) {
	start := time.Now()
	deferFunc := startNewSpanIfContextHasSpan(ctx, "db.ExecContext", db.ci.Application, db.ci.DBName, db.ci.Engine, query)
	defer deferFunc()
	res, err := db.conn.ExecContext(ctx, query, args...)
	logQuery(logger.FromContext(ctx), start, query, args, err)
	return res, err
}

// QueryContext runs the given sql statement using the given context, db.QueryContext, logs details about the call and returns the result
func (db *DB) QueryContext(ctx context.Context, query string, args ...interface{}) (*sql.Rows, error) {
	start := time.Now()
	deferFunc := startNewSpanIfContextHasSpan(ctx, "db.QueryContext", db.ci.Application, db.ci.DBName, db.ci.Engine, query)
	defer deferFunc()
	rows, err := db.conn.QueryContext(ctx, query, args...)
	logQuery(logger.FromContext(ctx), start, query, args, err)
	return rows, err
}

// PingContext pings the DB using the given context.
// Not logging the command to avoid spamming this in clients
// for health checks.
func (db *DB) PingContext(ctx context.Context) error {
	err := db.conn.PingContext(ctx)
	return err
}

// QueryRowContext runs the given sql statement using the given context db.QueryRowContext, logs details about the call and returns the result
func (db *DB) QueryRowContext(ctx context.Context, query string, args ...interface{}) *sql.Row {
	start := time.Now()
	deferFunc := startNewSpanIfContextHasSpan(ctx, "db.QueryRowContext", db.ci.Application, db.ci.DBName, db.ci.Engine, query)
	defer deferFunc()
	row := db.conn.QueryRowContext(ctx, query, args...)
	logQuery(logger.FromContext(ctx), start, query, args, nil)
	return row
}

//PrepareContext prepares a statement with the underlying context, database connection, logs, and then returns a statement
func (db *DB) PrepareContext(ctx context.Context, query string) (*Stmt, error) {
	start := time.Now()
	deferFunc := startNewSpanIfContextHasSpan(ctx, "db.Prepare", db.ci.Application, db.ci.DBName, db.ci.Engine, query)
	defer deferFunc()
	stmt, err := db.conn.PrepareContext(ctx, query)
	if err != nil {
		return nil, err
	}
	logger.FromContext(ctx).Infow("sql prepare", "sql.query", collapseSpaces(query), "sql.hash", hash(query), "query_time_ms", ms(time.Since(start)))
	return newStmt(stmt, query, db, logger.FromContext(ctx)), nil
}

func startNewSpanIfContextHasSpan(ctx context.Context, name, appName, dbName, dbEngine, query string) func() {
	if span := opentracing.SpanFromContext(ctx); span != nil {
		dbSpan := opentracing.StartSpan(name, opentracing.ChildOf(span.Context()))
		addCommonTagsToSpan(dbSpan, appName, dbName, dbEngine, query)
		return dbSpan.Finish
	}
	return func() {} // no-op because we do not have a span to finish
}

func addCommonTagsToSpan(span opentracing.Span, appName, dbName, dbEngine, query string) {
	ext.DBInstance.Set(span, dbName)
	ext.DBType.Set(span, dbEngine)
	ext.DBUser.Set(span, appName)
	ext.DBStatement.Set(span, query)
}

// Begin starts a transaction with the underlying database connection and returns it
func (db *DB) Begin() (*Tx, error) {
	db.log.Infow("sql transaction begin")
	tx, err := db.conn.Begin()
	if err != nil {
		return nil, err
	}
	return newTx(tx, db, db.log), nil
}

// Begin starts a transaction with context, options, the underlying database connection and returns it
func (db *DB) BeginTx(ctx context.Context, opts *sql.TxOptions) (*Tx, error) {
	db.log.Infow("sql transaction begin")
	tx, err := db.conn.BeginTx(ctx, opts)
	if err != nil {
		return nil, err
	}
	return newTx(tx, db, db.log), nil
}

//Close closes the underlying database connection
func (db *DB) Close() error {
	return db.conn.Close()
}

//DoInTransaction runs the give operation inside a transaction.The operation should use the Querier passed
//to it to make queries inside the transaction. If an error is returned from the operation, the transaction will be
//rolled back, otherwise it will be committed. If an error occurs while starting or committing the transaction,
//that error will be returned. If an error occurs while rolling back, that error will be logged and ignored,
//and the error returned from the operation will be returned
func (db *DB) DoInTransaction(operation func(Querier) error) error {
	tx, err := db.Begin()
	if err != nil {
		return err
	}

	err = operation(tx)
	if err == nil {
		err = tx.Commit()
	} else {
		rberr := tx.Rollback()
		if rberr != nil {
			db.log.Errorw("error while rolling back transaction", zap.Error(rberr))
		}
	}
	return err
}

//DoInTransaction runs the give operation inside a transaction with context.The operation should use the Querier passed
//to it to make queries inside the transaction. If an error is returned from the operation, the transaction will be
//rolled back, otherwise it will be committed. If an error occurs while starting or committing the transaction,
//that error will be returned. If an error occurs while rolling back, that error will be logged and ignored,
//and the error returned from the operation will be returned
func (db *DB) DoInTransactionContext(ctx context.Context, opts *sql.TxOptions, operation func(context.Context, Querier) error) error {

	tx, err := db.BeginTx(ctx, opts)
	if err != nil {
		return err
	}
	err = operation(ctx, tx)
	if err == nil {
		err = tx.Commit()
	} else {
		rberr := tx.Rollback()
		if rberr != nil {
			logger.FromContext(ctx).Errorw("error while rolling back transaction", zap.Error(rberr))
		}
	}
	return err
}
