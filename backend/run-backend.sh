#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/podman-compose.yml"
GRADLEW="${SCRIPT_DIR}/gradlew"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_PROJECT_NAME_DEFAULT="$(basename "${SCRIPT_DIR}")"

COLOR_RESET="\033[0m"
COLOR_BLUE="\033[1;34m"
COLOR_GREEN="\033[1;32m"
COLOR_YELLOW="\033[1;33m"
COLOR_RED="\033[1;31m"

log_info() { echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $*"; }
log_success() { echo -e "${COLOR_GREEN}[OK]${COLOR_RESET} $*"; }
log_warning() { echo -e "${COLOR_YELLOW}[WARN]${COLOR_RESET} $*"; }
log_error() { echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $*" >&2; }

COMPOSE_CMD=()
RUN_TESTS=false
RUN_BUILD=false
RUN_VERIFY=false
SUCCESS=false

cleanup() {
  local exit_code=$?
  if [[ "${SUCCESS}" == "true" ]]; then
    return
  fi

  log_warning "Backend startup failed. Stopping project containers."
  if [[ ${#COMPOSE_CMD[@]} -gt 0 ]]; then
    "${COMPOSE_CMD[@]}" -f "${COMPOSE_FILE}" down -v --remove-orphans >/dev/null 2>&1 || true
  fi
  exit "${exit_code}"
}
trap cleanup EXIT

require_command() {
  local command_name="$1"
  local hint="$2"
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    log_error "Missing command '${command_name}'. ${hint}"
    exit 1
  fi
}

setup_compose_command() {
  if podman compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(podman compose)
    return
  fi

  if command -v podman-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(podman-compose)
    return
  fi

  log_error "Neither 'podman compose' nor 'podman-compose' is available."
  exit 1
}

get_compose_project_candidates() {
  local env_project="${COMPOSE_PROJECT_NAME:-}"
  local script_project
  local root_project
  script_project="${COMPOSE_PROJECT_NAME_DEFAULT}"
  root_project="$(basename "${PROJECT_ROOT}")"

  if [[ -n "${env_project}" ]]; then
    printf "%s\n" "${env_project}" "${script_project}" "${root_project}"
  else
    printf "%s\n" "${script_project}" "${root_project}"
  fi | awk 'NF && !seen[$0]++'
}

get_postgres_container() {
  local project_name
  local id
  local name

  while IFS= read -r project_name; do
    while read -r id name; do
      [[ -z "${id:-}" ]] && continue
      if [[ "${name}" == *"postgres"* ]]; then
        echo "${id}"
        return 0
      fi
    done < <(podman ps \
      --filter "label=io.podman.compose.project=${project_name}" \
      --format "{{.ID}} {{.Names}}")
  done < <(get_compose_project_candidates)

  podman ps --format "{{.ID}} {{.Names}}" \
    | awk '$2=="seat-reservation-postgres"{print $1; exit}'
}

get_app_container() {
  local project_name
  local id
  local name

  while IFS= read -r project_name; do
    while read -r id name; do
      [[ -z "${id:-}" ]] && continue
      if [[ "${name}" == *"app"* || "${name}" == *"seat-reservation-app"* ]]; then
        echo "${id}"
        return 0
      fi
    done < <(podman ps \
      --filter "label=io.podman.compose.project=${project_name}" \
      --format "{{.ID}} {{.Names}}")
  done < <(get_compose_project_candidates)

  podman ps --format "{{.ID}} {{.Names}}" \
    | awk '$2=="seat-reservation-app"{print $1; exit}'
}

ensure_podman_machine() {
  local machine_name
  machine_name="$(podman machine list --format "{{.Name}}" | head -n1 || true)"
  if [[ -z "${machine_name}" ]]; then
    log_error "No Podman machine found. Run: podman machine init"
    exit 1
  fi

  local running
  running="$(podman machine list --format "{{if eq .Name \"${machine_name}\"}}{{.Running}}{{end}}" | tr -d '[:space:]')"
  if [[ "${running}" != "true" ]]; then
    log_info "Starting Podman machine '${machine_name}'..."
    podman machine start "${machine_name}"
  fi

  local attempt
  for attempt in $(seq 1 30); do
    if podman info >/dev/null 2>&1; then
      log_success "Podman machine is healthy."
      return
    fi
    sleep 1
  done

  log_error "Podman machine did not become healthy."
  exit 1
}

run_backend_tests() {
  log_info "Running backend tests (clean test)..."
  "${GRADLEW}" --no-daemon clean test

  log_info "Running critical backend integration tests..."
  "${GRADLEW}" --no-daemon test \
    --tests com.linkz.reservation.payment.PaymentProcessingIntegrationTest \
    --tests com.linkz.reservation.commons.exception.ApiErrorResponseIntegrationTest \
    --tests com.linkz.reservation.scheduler.ReservationSchedulerIntegrationTest \
    --tests com.linkz.reservation.repository.RepositoryIntegrationTest
  log_success "Backend tests passed."
}

run_backend_build() {
  log_info "Building backend (clean build)..."
  "${GRADLEW}" --no-daemon clean build
  log_success "Backend build passed."
}

wait_for_postgres() {
  log_info "Waiting for PostgreSQL container and readiness..."

  local postgres_container_id
  local elapsed=0
  local timeout=120
  while (( elapsed < timeout )); do
    postgres_container_id="$(get_postgres_container)"
    if [[ -n "${postgres_container_id}" ]]; then
      break
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  if [[ -z "${postgres_container_id}" ]]; then
    log_error "PostgreSQL container not found."
    exit 1
  fi

  elapsed=0
  while (( elapsed < timeout )); do
    if podman exec "${postgres_container_id}" pg_isready -U postgres >/dev/null 2>&1; then
      log_success "PostgreSQL is ready."
      return
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  if ! podman exec "${postgres_container_id}" pg_isready -U postgres >/dev/null 2>&1; then
    log_error "PostgreSQL did not become ready."
    exit 1
  fi
}

wait_for_backend() {
  log_info "Waiting for backend container and health endpoint..."

  local app_container_id
  local elapsed=0
  local startup_timeout=120
  while (( elapsed < startup_timeout )); do
    app_container_id="$(get_app_container)"
    if [[ -n "${app_container_id}" ]]; then
      break
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  if [[ -z "${app_container_id}" ]]; then
    log_error "Backend app container not found."
    exit 1
  fi

  local healthy=false
  elapsed=0
  while (( elapsed < 180 )); do
    local payload
    payload="$(curl -fsS "http://localhost:8080/actuator/health" 2>/dev/null || true)"
    if [[ -n "${payload}" ]] && [[ "$(jq -r '.status // empty' <<<"${payload}")" == "UP" ]]; then
      healthy=true
      break
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  if [[ "${healthy}" != "true" ]]; then
    log_error "Backend /actuator/health did not return status UP in time."
    exit 1
  fi

  log_success "Backend health endpoint is UP."
}

verify_database_connectivity() {
  log_info "Verifying database connectivity and Liquibase tables..."
  local postgres_container_id
  postgres_container_id="$(get_postgres_container)"
  if [[ -z "${postgres_container_id}" ]]; then
    log_error "Cannot verify database because PostgreSQL container is missing."
    exit 1
  fi

  podman exec "${postgres_container_id}" \
    psql -U reservation_user -d reservation_db -tAc "SELECT 1;" >/dev/null

  local dbchangelog
  dbchangelog="$(podman exec "${postgres_container_id}" \
    psql -U reservation_user -d reservation_db -tAc "SELECT to_regclass('public.databasechangelog');" | tr -d '[:space:]')"
  local dbchangeloglock
  dbchangeloglock="$(podman exec "${postgres_container_id}" \
    psql -U reservation_user -d reservation_db -tAc "SELECT to_regclass('public.databasechangeloglock');" | tr -d '[:space:]')"

  if [[ "${dbchangelog}" != "databasechangelog" || "${dbchangeloglock}" != "databasechangeloglock" ]]; then
    log_error "Liquibase tables were not found in reservation_db."
    exit 1
  fi

  log_success "Database connection and Liquibase tables verified."
}

start_stack() {
  log_info "Stopping previous project containers safely..."
  "${COMPOSE_CMD[@]}" -f "${COMPOSE_FILE}" down -v --remove-orphans

  log_info "Starting PostgreSQL..."
  "${COMPOSE_CMD[@]}" -f "${COMPOSE_FILE}" up -d postgres
  wait_for_postgres

  log_info "Starting backend container (Liquibase migrations run on startup)..."
  "${COMPOSE_CMD[@]}" -f "${COMPOSE_FILE}" up -d app
  wait_for_backend
  verify_database_connectivity
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --with-tests) RUN_TESTS=true ;;
      --with-build) RUN_BUILD=true ;;
      --verify) RUN_VERIFY=true ;;
      *)
        log_error "Unknown option: $1"
        exit 1
        ;;
    esac
    shift
  done
}

print_summary() {
  cat <<EOF

Backend ready:
  API:      http://localhost:8080
  Swagger:  http://localhost:8080/swagger-ui/index.html
  Health:   http://localhost:8080/actuator/health
  Database: postgresql://localhost:5432/reservation_db
EOF
}

main() {
  parse_args "$@"

  require_command "podman" "Install Podman first."
  require_command "curl" "Install curl first."
  require_command "jq" "Install jq first."
  require_command "java" "Install Java 21 first."
  setup_compose_command
  ensure_podman_machine

  cd "${SCRIPT_DIR}"
  [[ -x "${GRADLEW}" ]] || chmod +x "${GRADLEW}"

  if [[ "${RUN_TESTS}" == "true" ]]; then
    run_backend_tests
  fi

  if [[ "${RUN_BUILD}" == "true" ]]; then
    run_backend_build
  fi

  start_stack

  if [[ "${RUN_VERIFY}" == "true" ]]; then
    wait_for_backend
    verify_database_connectivity
  fi

  print_summary
  SUCCESS=true
}

main "$@"
