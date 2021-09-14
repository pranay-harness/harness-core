// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package stream

import (
	"github.com/wings-software/portal/product/log-service/client"

	"gopkg.in/alecthomas/kingpin.v2"
)

type openCommand struct {
	key       string
	accountID string
	server    string
	token     string
}

func (c *openCommand) run(*kingpin.ParseContext) error {
	client := client.NewHTTPClient(c.server, c.accountID, c.token, false)
	return client.Open(nocontext, c.key)
}

func registerOpen(app *kingpin.CmdClause) {
	c := new(openCommand)

	cmd := app.Command("open", "open the log stream").
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
