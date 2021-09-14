// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package stream

import (
	"github.com/wings-software/portal/product/log-service/client"
	"github.com/wings-software/portal/product/log-service/stream"

	"gopkg.in/alecthomas/kingpin.v2"
)

type pushCommand struct {
	key       string
	accountID string
	server    string
	token     string
	line      *stream.Line
}

func (c *pushCommand) run(*kingpin.ParseContext) error {
	client := client.NewHTTPClient(c.server, c.accountID, c.token, false)
	return client.Write(nocontext, c.key, []*stream.Line{c.line})
}

func registerPush(app *kingpin.CmdClause) {
	c := new(pushCommand)
	c.line = new(stream.Line)

	cmd := app.Command("push", "push log lines").
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

	cmd.Arg("message", "line message").
		StringVar(&c.line.Message)

	cmd.Arg("level", "severity of message").
		Default("info").
		StringVar(&c.line.Level)

	cmd.Arg("line", "line number").
		Default("0").
		IntVar(&c.line.Number)

	cmd.Flag("server", "server endpoint").
		Default("http://localhost:8079").
		StringVar(&c.server)
}
