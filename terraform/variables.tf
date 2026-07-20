variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "prod"
}

variable "app_name" {
  description = "Application name, used as a resource name prefix"
  type        = string
  default     = "papertrade"
}

# --- Networking ----------------------------------------------------------
variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

# --- Backend service -----------------------------------------------------
variable "backend_image_tag" {
  description = "Container image tag for the backend (set by CI)"
  type        = string
  default     = "latest"
}

variable "backend_cpu" {
  description = "Fargate CPU units for the backend task"
  type        = number
  default     = 512
}

variable "backend_memory" {
  description = "Fargate memory (MiB) for the backend task"
  type        = number
  default     = 1024
}

variable "backend_desired_count" {
  description = "Number of backend tasks to run"
  type        = number
  default     = 1
}

# --- Database ------------------------------------------------------------
variable "db_name" {
  description = "Postgres database name"
  type        = string
  default     = "papertrade"
}

variable "db_username" {
  description = "Postgres master username"
  type        = string
  default     = "papertrade"
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t4g.micro"
}

# --- Redis ---------------------------------------------------------------
variable "redis_node_type" {
  description = "ElastiCache node type"
  type        = string
  default     = "cache.t4g.micro"
}

# --- Application secrets (provide via TF_VAR_* or a tfvars file, never commit) ---
variable "jwt_secret" {
  description = "JWT signing secret"
  type        = string
  sensitive   = true
}

variable "finnhub_api_key" {
  description = "Finnhub API key"
  type        = string
  sensitive   = true
}

variable "twelve_data_api_key" {
  description = "Twelve Data API key"
  type        = string
  sensitive   = true
}
