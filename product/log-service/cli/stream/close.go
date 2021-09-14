// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package stream

import (
	"github.com/wings-software/portal/product/log-service/client"
	"gopkg.in/alecthomas/kingpin.v2"
)

type closeCommand struct {
	key       string
	accountID string
	server    string
	token     string
}

func (c *closeCommand) run(*kingpin.ParseContext) error {
	client := client.NewHTTPClient(c.server, c.accountID, c.token, false)
	return client.Close(nocontext, c.key)
}

func registerClose(app *kingpin.CmdClause) {
	c := new(closeCommand)

	cmd := app.Command("close", "close the log stream").
		Action(c.run)

	cmd.Arg("accountID", "project identifier").
		Required().
		StringVar(&c.accountID)

	cmd.Arg("token", "server token").
		Required().
		StringVar(&c.token)

	cmd.Arg("key", "key identifier").
		Required().
		StringVar(&c.key)

	cmd.Flag("server", "server endpoint").
		Default("http://localhost:8079").
		StringVar(&c.server)

}
