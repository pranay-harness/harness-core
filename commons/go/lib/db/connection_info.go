package db

import (
	"database/sql"
	"fmt"

	gomysql "github.com/go-sql-driver/mysql"
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
	return fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=disable application_name=%s",
		ci.Host, ci.Port, ci.User, ci.Password, ci.DBName, ci.Application)
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
