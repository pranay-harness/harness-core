// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package main

import (
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"time"

	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
)

var logger *log.Logger

type StdStreamJoint struct {
	in     io.Reader
	out    io.Writer
	closed bool
	local  *StdinAddr
	remote *StdinAddr
}

type StdinAddr struct {
	s string
}

func (s *StdStreamJoint) Close() error { //nolint
	s.closed = true
	return nil
}

func (a *StdinAddr) Network() string {
	return "stdio"
}
func (s *StdStreamJoint) Write(b []byte) (n int, err error) {
	return s.out.Write(b)
}

func (a *StdinAddr) String() string {
	return a.s
}

func (s *StdStreamJoint) LocalAddr() net.Addr {
	return s.local
}

func (s *StdStreamJoint) RemoteAddr() net.Addr {
	return s.remote
}

func (s *StdStreamJoint) SetDeadline(t time.Time) error {
	return nil
}

func (s *StdStreamJoint) SetReadDeadline(t time.Time) error {
	return nil
}

func (s *StdStreamJoint) SetWriteDeadline(t time.Time) error {
	return nil
}
func (s *StdStreamJoint) Read(b []byte) (n int, err error) {
	return s.in.Read(b)
}

func main() {
	debugFile, err := os.Create("client.log")
	if err != nil {
		panic(err)
	}
	logger = log.New(debugFile, "", log.LstdFlags)

	logger.Println("client starting")
	conn, err := grpc.Dial("unix:///tmp/bla", grpc.WithInsecure())
	if err != nil {
		logger.Fatalf("did not connect: %v", err)
	}
	defer conn.Close()
	c := pb.NewSCMClient(conn)

	ctx, cancel := context.WithTimeout(context.Background(), (time.Second * 10))
	defer cancel()
	in := &pb.GetFileRequest{
		Slug: "tphoney/scm-test",
		Path: "README.md",
		Type: &pb.GetFileRequest_Branch{
			Branch: "main",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
					},
				},
			},
			Debug: true,
		},
	}

	r, err := c.GetFile(ctx, in)
	if err != nil {
		logger.Fatalf("could not greet: %v", err)
	}
	fmt.Printf("content: %v", r)
}
