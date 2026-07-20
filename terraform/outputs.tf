output "api_endpoint" {
  description = "Public API base URL (point the frontend at this)"
  value       = "http://${aws_lb.main.dns_name}"
}

output "alb_dns_name" {
  description = "ALB DNS name"
  value       = aws_lb.main.dns_name
}

output "ecr_repository_url" {
  description = "ECR repo to push the backend image to (used by CI)"
  value       = aws_ecr_repository.backend.repository_url
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.backend.name
}

output "order_queue_url" {
  description = "SQS order queue URL"
  value       = aws_sqs_queue.orders.url
}

output "rds_endpoint" {
  description = "RDS Postgres endpoint"
  value       = aws_db_instance.postgres.address
}

output "redis_endpoint" {
  description = "ElastiCache Redis endpoint"
  value       = aws_elasticache_cluster.redis.cache_nodes[0].address
}
