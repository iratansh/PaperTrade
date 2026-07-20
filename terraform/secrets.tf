# Application secrets stored as SSM SecureString parameters and injected into
# the ECS task at runtime (never baked into the image or committed to source).

resource "aws_ssm_parameter" "db_password" {
  name  = "/${local.name}/DB_PASSWORD"
  type  = "SecureString"
  value = random_password.db.result
}

resource "aws_ssm_parameter" "jwt_secret" {
  name  = "/${local.name}/JWT_SECRET"
  type  = "SecureString"
  value = var.jwt_secret
}

resource "aws_ssm_parameter" "finnhub_api_key" {
  name  = "/${local.name}/FINNHUB_API_KEY"
  type  = "SecureString"
  value = var.finnhub_api_key
}

resource "aws_ssm_parameter" "twelve_data_api_key" {
  name  = "/${local.name}/TWELVE_DATA_API_KEY"
  type  = "SecureString"
  value = var.twelve_data_api_key
}
