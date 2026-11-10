#!/bin/sh
# Ajusta o autor de todos os commits de todas as branches para o seu nome e e-mail do GitHub.
# Uso: sh scripts/ajustar-autor.sh "Seu Nome" "seu.email@exemplo.com"
# Execute ANTES do primeiro push.
set -e
if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Uso: sh scripts/ajustar-autor.sh \"Seu Nome\" \"seu.email@exemplo.com\""
  exit 1
fi
NOME="$1"; EMAIL="$2"
git config user.name "$NOME"
git config user.email "$EMAIL"
FILTER_BRANCH_SQUELCH_WARNING=1 git filter-branch -f --env-filter "
export GIT_AUTHOR_NAME=\"$NOME\"
export GIT_AUTHOR_EMAIL=\"$EMAIL\"
export GIT_COMMITTER_NAME=\"$NOME\"
export GIT_COMMITTER_EMAIL=\"$EMAIL\"
" --tag-name-filter cat -- --all
echo "Autor ajustado em todas as branches e tags."
