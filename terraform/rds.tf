resource "random_password" "db" {
  length  = 24
  special = false
}

resource "aws_db_subnet_group" "main" {
  name       = "${local.name}-db"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_db_instance" "postgres" {
  identifier     = "${local.name}-postgres"
  engine         = "postgres"
  engine_version = "15"
  instance_class = var.db_instance_class

  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.db_name
  username = var.db_username
  password = random_password.db.result

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  multi_az               = false # set true for prod HA

  backup_retention_period = 7
  skip_final_snapshot     = true # convenient for teardown; set false for prod
  deletion_protection     = false

  performance_insights_enabled = false
}
