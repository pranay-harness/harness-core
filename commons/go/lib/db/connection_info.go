// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
	SSLMode     string `json:"ssl_mode"`
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
		// require mode -- disable doesn't works for onprem as server rejects the connection
		return fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=%s application_name=%s",
			ci.Host, ci.Port, ci.User, ci.Password, ci.DBName, ci.SSLMode, ci.Application)
	} else {
		return fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=%s sslrootcert=%s sslcert= sslkey= application_name=%s",
			ci.Host, ci.Port, ci.User, ci.Password, ci.DBName, ci.SSLMode, ci.SSLCertPath, ci.Application)
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
