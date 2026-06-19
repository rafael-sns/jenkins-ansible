# jenkins-ansible

Repositório GitLab com inventário Ansible, playbooks de coleta de logs e script Jenkins para execução no agente **ansible-pod** (infra existente).

## Estrutura

```
jenkins-ansible/
├── ansible.cfg
├── Jenkinsfile
├── scripts/
│   ├── resolve-log-period.groovy
│   └── load-inventory.groovy
└── job/
    ├── inventories/
    │   └── hosts.yaml
    └── playbooks/
        ├── playbook-test-connectivity.yaml   # pwd + df -h (teste SSH)
        ├── playbook-host-1.yaml
        └── playbook-host-2.yaml
```

## Credenciais Jenkins

| ID | Tipo | Onde |
|----|------|------|
| `gitlab-repo` | Username/Password ou SSH | SCM do job (clone/pull GitLab) |
| `host-1-ssh`, `host-2-ssh`, … | SSH Username with private key | Definidos por host no inventário (`jenkins_ssh_credential`) |

Cada host declara sua credencial no inventário. O Jenkins usa `sshagent` com a chave correspondente antes de executar o playbook daquele host.

### Exemplo de inventário

```yaml
all:
  hosts:
    host-1:
      ansible_host: 10.0.0.1
      ansible_user: deploy
      jenkins_ssh_credential: host-1-ssh
    host-2:
      ansible_host: 10.0.0.2
      ansible_user: appuser
      jenkins_ssh_credential: host-2-ssh
```

| Variável | Usado por | Descrição |
|----------|-----------|-----------|
| `ansible_host` | Ansible | IP ou hostname do servidor |
| `ansible_user` | Ansible | Usuário SSH |
| `jenkins_ssh_credential` | Jenkins | ID da credencial SSH no Jenkins (não usado pelo Ansible em runtime) |

## Plugins Jenkins necessários

- **SSH Agent Plugin** — `sshagent` no Pipeline
- **Pipeline Utility Steps** — `readYaml` para ler o inventário

## Parâmetro LOG_PERIOD

O `Jenkinsfile` declara `choice` nativo com `TODAY | YESTERDAY | TOTAL`.

Alternativa com plugin **Active Choices** (opções com data na UI):

```
1 - Data atual (dd/MM/yyyy)
2 - Data anterior (dd/MM/yyyy)
3 - Total (todo o conteúdo disponível)
```

O script `resolve-log-period.groovy` normaliza ambos os formatos.

## Registrar o job no Jenkins existente

1. Criar job **Pipeline** apontando SCM para este repositório GitLab.
2. **Pipeline script from SCM** → `Jenkinsfile` na raiz.
3. Cadastrar credenciais SSH (chave privada) com IDs iguais aos de `jenkins_ssh_credential` no inventário.
4. Cadastrar credencial `gitlab-repo` para o SCM.
5. Confirmar que o nó `ansible-pod` está **online**.
6. Substituir IPs e IDs de credencial em `job/inventories/hosts.yaml`.
7. Executar um build de teste.

## Fluxo do build

1. Aloca agente `node('ansible-pod')`.
2. Checkout + `git pull` para sincronizar playbooks.
3. `resolve-log-period.groovy` converte `LOG_PERIOD` em `log_period` / `log_date`.
4. **Testar conexão** — `playbook-test-connectivity.yaml` (`pwd` + `df -h`) em cada host.
5. Lê `hosts.yaml` e, para cada host, executa `sshagent([jenkins_ssh_credential])` + playbook de coleta.
6. Publicação de `artifacts/**/*` (quando os playbooks implementarem a coleta).

### Teste manual (ansible-pod)

```bash
ansible-playbook \
  -i job/inventories/hosts.yaml \
  job/playbooks/playbook-test-connectivity.yaml \
  --limit host-1
```

## Validação no agente ansible-pod

```bash
ansible-playbook \
  -i job/inventories/hosts.yaml \
  job/playbooks/playbook-host-1.yaml \
  --limit host-1 \
  --syntax-check
```

## Próximos passos

- Substituir IPs e IDs de credencial em `hosts.yaml`.
- Implementar tasks reais de coleta nos playbooks (substituir placeholders `debug`).
- Ajustar `log_source_path` no `Jenkinsfile` conforme caminho real dos logs nos hosts.
