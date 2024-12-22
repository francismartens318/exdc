enterContainer() {
     docker exec -it $1 bash
}


alias dc="docker-compose"
alias dcrm="docker-compose rm"
alias dcup="docker-compose up -d"
alias dcstop="docker-compose stop"
alias dcps="docker-compose ps"
alias dclog="docker-compose logs --tail=100 -f "
alias dcpsl='docker ps --format "{{.Names}}" | sort '
alias dcname='docker inspect -f {{.Name}}'
alias dcstat='docker stats '
alias dcec="enterContainer"
alias dcip="docker inspect -f '{{ .NetworkSettings.IPAddress }}'"
alias dcnames="docker ps -q | xargs docker inspect -f '{{.State.Pid}} : {{.Name}}'"
alias dcserv="grep '^  [a-z]*:'  docker-compose.yml"
alias dctop="ctop -scale-cpu -s cpu"
alias dcex='docker-compose exec '

alias edb='docker-compose exec database psql -U exalate'
