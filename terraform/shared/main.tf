resource "aws_key_pair" "dev_test" {
  public_key = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDUHGzV0xBIPblUGtNW8y9MXBNqAv432ooCm3F0EFG/EWb0p+9OxkCJu18WBdrvWDLuJ364wM8ZAQe0h5de/ORg+r8w+W1WihaGi5p0bOs6vWFUMcxQbAMZCunLtfJk98cqOMaY1R34HbvMOiflG05lsJtEK96+g+cK9O0i19IUuIX+YT6r///WOnJ/qMs/P++kBEuvZWhkM2/GFT9F0k5JdBWTERArMNQxOyAJNmSHXFz01vWB8O93UF8m/qX7aeHaxaZFLLvaVu6nENxQbkJQ8PhX+Rr125V8YqbKWj1CiiJbkRHSprSUIcZvOKXzFFnBHiCoBy3thTx1rUUQFYaV george@example.com"
}

resource "aws_security_group" "dev_test" {
  description = "dev_test security group allows ssh for everyone"

  ingress {
    from_port = 22
    to_port = 22
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

output "aws_key_pair_id" {
  value = "${aws_key_pair.dev_test.id}"
}

output "aws_security_group_name" {
  value = "${aws_security_group.dev_test.name}"
}