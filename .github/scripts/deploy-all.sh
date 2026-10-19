#!/usr/bin/env bash
set -Eeuo pipefail

: "${AWS_REGION:?AWS_REGION is required}"
: "${DEPLOY_INSTANCE_IDS:?DEPLOY_INSTANCE_IDS must be a space-separated list of SSM-managed EC2 instance IDs}"
: "${DEPLOY_ENV_PARAMETER:?DEPLOY_ENV_PARAMETER is required}"
: "${IMAGE:?IMAGE is required}"
: "${1:?service name is required}"

service="$1"
service_name="${2:-$service}"
host_path="${DEPLOY_PATH:-/home/ubuntu/app}"
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
encoded_script="$(base64 -w0 "${script_dir}/deploy-instance.sh")"
failed_instances=()

for instance_id in $DEPLOY_INSTANCE_IDS; do
  if [[ ! "$instance_id" =~ ^i-[0-9a-fA-F]+$ ]]; then
    echo "Invalid EC2 instance ID: ${instance_id}" >&2
    failed_instances+=("${instance_id}")
    continue
  fi

  printf -v remote_command \
    'printf %%s %q | base64 -d > /tmp/deploy-instance.sh && chmod 700 /tmp/deploy-instance.sh && sudo bash /tmp/deploy-instance.sh %q %q %q %q %q %q' \
    "$encoded_script" "$service" "$host_path" "$IMAGE" "$AWS_REGION" "$DEPLOY_ENV_PARAMETER" "$service_name"
  parameters="$(jq -cn --arg command "$remote_command" '{commands: [$command]}')"

  echo "Starting ${service} deployment (${IMAGE}) on ${instance_id}"
  command_id="$(aws ssm send-command \
    --region "$AWS_REGION" \
    --instance-ids "$instance_id" \
    --document-name AWS-RunShellScript \
    --comment "Deploy ${service}" \
    --parameters "$parameters" \
    --query 'Command.CommandId' \
    --output text)" || {
      echo "Could not send deployment command to ${instance_id}" >&2
      failed_instances+=("${instance_id}")
      continue
    }

  status="Pending"
  while [[ "$status" == "Pending" || "$status" == "InProgress" || "$status" == "Delayed" ]]; do
    sleep 5
    status="$(aws ssm get-command-invocation \
      --region "$AWS_REGION" \
      --command-id "$command_id" \
      --instance-id "$instance_id" \
      --query 'Status' \
      --output text 2>/dev/null || echo Pending)"
  done

  aws ssm get-command-invocation \
    --region "$AWS_REGION" \
    --command-id "$command_id" \
    --instance-id "$instance_id" \
    --query '{Status:Status,Output:StandardOutputContent,Error:StandardErrorContent}' \
    --output yaml

  if [[ "$status" != "Success" ]]; then
    echo "Deployment failed on ${instance_id} (${status})" >&2
    failed_instances+=("${instance_id}")
  fi
done

if ((${#failed_instances[@]} > 0)); then
  printf 'Deployment failed on: %s\n' "${failed_instances[*]}" >&2
  exit 1
fi

echo "${service} deployment completed on all configured instances."
