# Order execution queue + dead-letter queue.
# The backend enqueues order ids here; the SQS consumer executes the fills.

resource "aws_sqs_queue" "order_dlq" {
  name                      = "${local.name}-order-dlq"
  message_retention_seconds = 1209600 # 14 days
}

resource "aws_sqs_queue" "orders" {
  name                       = "${local.name}-orders"
  visibility_timeout_seconds = 60
  message_retention_seconds  = 86400 # 1 day
  receive_wait_time_seconds  = 10    # long polling

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.order_dlq.arn
    maxReceiveCount     = 5
  })
}
