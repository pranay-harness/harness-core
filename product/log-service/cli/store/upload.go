// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package store

import (
	"os"

	"github.com/wings-software/portal/product/log-service/client"
	"gopkg.in/alecthomas/kingpin.v2"
)

type uploadCommand struct {
	key       string
	path      string
	accountID string
	server    string
	token     string
}

func (c *uploadCommand) run(*kingpin.ParseContext) error {
	f, err := os.Open(c.path)
	if err != nil {
		return err
	}
	defer f.Close()
	client := client.NewHTTPClient(c.server, c.accountID, c.token, false)
	return client.Upload(nocontext, c.key, f)
}

func registerUpload(app *kingpin.CmdClause) {
	c := new(uploadCommand)

	cmd := app.Command("upload", "upload logs").
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

	cmd.Arg("path", "path to file to upload").
		Required().
		StringVar(&c.path)

	cmd.Flag("server", "server endpoint").
		Default("http://localhost:8079").
		StringVar(&c.server)
}
