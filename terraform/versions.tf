terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.40"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Remote state is recommended for a real deployment. Left commented so the
  # config can be `terraform init`'d locally without an existing bucket.
  # backend "s3" {
  #   bucket = "papertrade-tfstate"
  #   key    = "papertrade/terraform.tfstate"
  #   region = "us-east-1"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "papertrade"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
