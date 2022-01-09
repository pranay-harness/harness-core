// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package db

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"go.uber.org/zap/zaptest/observer"
	"gopkg.in/DATA-DOG/go-sqlmock.v1"
)

func Test_Tx(t *testing.T) {
	const (
		querySQL    = "SELECT a FROM table;"
		updateSQL   = "UPDATE table SET a = $1 WHERE b = $2;"
		queryRowSQL = "SELECT a FROM table WHERE b = $1;"
	)

	core, logs := observer.New(zapcore.InfoLevel)
	log := zap.New(core).Sugar()

	db, mock, err := NewMockDB(log)
	require.NoError(t, err, "No error expected when opening stub database connection")

	mock.MatchExpectationsInOrder(true)

	mock.ExpectBegin()
	tx, err := db.Begin()
	assert.NoError(t, err, "beginning a transaction shouldn't error")

	mock.ExpectExec("UPDATE table").WithArgs("a", "b").WillReturnResult(sqlmock.NewResult(1, 1))
	_, err = tx.Exec(updateSQL, "a", "b")
	assert.NoError(t, err, "exec shouldn't error")

	mock.ExpectQuery(querySQL).WillReturnRows(sqlmock.NewRows([]string{"a"}).AddRow("a"))
	_, err = tx.Query(querySQL)
	assert.NoError(t, err, "query shouldn't error")

	mock.ExpectQuery("SELECT a FROM table WHERE b =").WithArgs("b").WillReturnRows(sqlmock.NewRows([]string{"a"}).AddRow("a"))
	_ = tx.QueryRow(queryRowSQL, "b")
	assert.NoError(t, err, "query shouldn't error")

	mock.ExpectCommit()
	err = tx.Commit()
	assert.NoError(t, err, "commit shouldn't error")

	mock.ExpectBegin()
	tx, err = db.Begin()
	assert.NoError(t, err, "beginning transaction shouldn't error")

	mock.ExpectPrepare("UPDATE table")
	_, err = tx.Prepare(updateSQL)
	assert.NoError(t, err, "prepare shouldn't error")

	mock.ExpectRollback()
	err = tx.Rollback()
	assert.NoError(t, err, "rollback shouldn't error")

	mock.ExpectClose()
	err = db.Close()
	assert.NoError(t, err, "close shouldn't error")

	assert.NoError(t, mock.ExpectationsWereMet())
	assert.Len(t, logs.TakeAll(), 8)

}
