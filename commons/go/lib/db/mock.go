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
