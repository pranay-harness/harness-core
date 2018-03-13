# Configure the AWS Provider
variable "user" {}
variable "aws_key_pair_id" {}
variable "aws_security_group_name" {}

# Create an EC2 instance
resource "aws_instance" "test-workflow" {
    # AMI ID for Ubuntu Linux
    ami             = "ami-41e0b93b"
    instance_type   = "t2.micro"
    key_name        = "${var.aws_key_pair_id}"
    security_groups = ["${var.aws_security_group_name}"]

    tags {
        Purpose     = "test"
        Name        = "workflow"
        User        = "${var.user}"
    }

    count = 2
}