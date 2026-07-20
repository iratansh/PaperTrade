# PaperTrade — AWS Infrastructure (Terraform)

Infrastructure-as-code for the PaperTrade **backend**. The frontend is not
deployed to AWS; run it wherever you like and point it at the API endpoint this
stack outputs.

> This is written as a portfolio/interview artifact. You can `terraform plan` it
> to review the graph without spending anything. `terraform apply` provisions
> real, billable resources (RDS, ElastiCache, Fargate, ALB, NAT gateway).

## What it provisions

```
Internet
   │
   ▼
Application Load Balancer  (public subnets, :80)
   │
   ▼
ECS Fargate service  ──►  the backend container (private subnets)
   │  │  │
   │  │  └──► SQS  (order queue + dead-letter queue)
   │  └─────► ElastiCache Redis  (market-data cache)
   └────────► RDS Postgres        (private)

Secrets: SSM Parameter Store (SecureString) → injected into the ECS task
Images:  ECR  (built & pushed by GitHub Actions)
Logs:    CloudWatch
```

- **VPC** — 2 AZs, public + private subnets, single NAT gateway.
- **ECS Fargate** — runs the backend with `SPRING_PROFILES_ACTIVE=aws`.
- **ALB** — public entrypoint, health-checks `/actuator/health`.
- **RDS Postgres 15** / **ElastiCache Redis 7** — private, reachable only from the tasks.
- **SQS** — the order-execution queue the `SqsOrderQueue`/`SqsOrderConsumer` use.
- **IAM** — least-privilege task execution role (ECR + SSM + logs) and task role (SQS).
- **Security groups** — tight chaining: ALB→backend→(RDS, Redis); nothing else is reachable.

## Usage

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars   # fill in secrets (git-ignored)

terraform init
terraform plan      # review — no cost

# Optional — provisions billable resources:
terraform apply
terraform output api_endpoint
```

Then build & push the backend image (or let CI do it) and the ECS service picks it up:

```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ecr_url>
docker build -t <ecr_url>:latest ./backend
docker push <ecr_url>:latest
aws ecs update-service --cluster <cluster> --service <service> --force-new-deployment
```

Tear it all down when finished:

```bash
terraform destroy
```

## Notes / production hardening

- **HTTPS**: the ALB listens on HTTP:80. For real use, add an ACM certificate + an
  HTTPS:443 listener and redirect 80→443.
- **HA**: `single_nat_gateway = true`, `multi_az = false`, and one Redis node keep
  cost down. Flip these for production high availability.
- **CI auth**: the workflow uses access keys; prefer GitHub OIDC + an IAM role.
- **Secrets** never touch the image or git — they live in SSM and are pulled at task start.
