// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package db

import (
	"database/sql"
	"fmt"

	gomysql "github.com/go-sql-driver/mysql"
	_ "github.com/lib/pq"
)

// ConnectionInfo contains information to connect to a database
type ConnectionInfo struct {
	Application string
	DBName      string
	User        string `json:"username" validate:"nonzero"`
	Password    string `json:"password" validate:"nonzero"`
	Host        string `json:"host" validate:"nonzero"`
	Port        uint   `json:"port" validate:"nonzero"`
	Engine      string `json:"engine" validate:"nonzero"`
	EnableSSL   bool   `json:"enable_ssl"`
	SSLCertPath string `json:"ssl_cert_path"`
}

func (ci *ConnectionInfo) getDBConnection() (*sql.DB, error) {
	return sql.Open(ci.Engine, ci.String())
}

// String returns the connection info formatted as a connection string based on its Engine
func (ci *ConnectionInfo) String() string {
	switch ci.Engine {
	case "postgres":
		return ci.psqlConnectionString()
	case "mysql":
		return ci.mysqlConnectionString()
	}
	return ""
}

func (ci *ConnectionInfo) psqlConnectionString() string {
	if ci.EnableSSL == false {
		return fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=disable application_name=%s",
			ci.Host, ci.Port, ci.User, ci.Password, ci.DBName, ci.Application)
	} else {
		return fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=verify-full sslrootcert=%s sslcert= sslkey= application_name=%s",
			ci.Host, ci.Port, ci.User, ci.Password, ci.DBName, ci.SSLCertPath, ci.Application)
	}

}

func (ci *ConnectionInfo) mysqlConnectionString() string {
	config := gomysql.NewConfig()
	config.Net = "tcp"
	config.User = ci.User
	config.Passwd = ci.Password
	config.Addr = fmt.Sprintf("%s:%d", ci.Host, ci.Port)
	config.DBName = ci.DBName

	return config.FormatDSN()
}
