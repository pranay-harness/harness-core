// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package db

import (
	"go.uber.org/zap"
	"gopkg.in/DATA-DOG/go-sqlmock.v1"
)

//NewMockDB returns a client with a mock connection that can be used for testing purpose
func NewMockDB(log *zap.SugaredLogger) (*DB, sqlmock.Sqlmock, error) {
	conn, mock, err := sqlmock.New()
	if err != nil {
		return nil, nil, err
	}
	db := &DB{
		conn: conn,
		log:  log,
	}
	return db, mock, nil
}
