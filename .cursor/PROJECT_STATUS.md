# Status do projeto

## Última atualização (2026-06-19)

### Feito

- Estrutura Ansible: `ansible.cfg`, `job/inventories/hosts.yaml`, playbooks placeholder `host-1` / `host-2`.
- Script Jenkins: `Jenkinsfile` com `node('ansible-pod')`, sync Git, resolver período, `sshagent` por host, loop `ansible-playbook`.
- Playbook de teste: `playbook-test-connectivity.yaml` (`pwd`, `df -h`); stage no Jenkinsfile.
- Scripts auxiliares: `resolve-log-period.groovy`, `load-inventory.groovy`.
- Inventário com `ansible_user` e `jenkins_ssh_credential` por host.
- README atualizado.

### Decisões

- Credenciais SSH por host via `jenkins_ssh_credential` no inventário + `sshagent` no Pipeline.
- `ansible_user` no inventário (Ansible lê automaticamente); chave privada via SSH agent, sem senha em `-e`.
- Execução via agente Jenkins `node('ansible-pod')`.
- Playbooks com tasks placeholder; coleta real pendente.

### Próximos passos

- Configurar IPs e IDs de credencial reais em `job/inventories/hosts.yaml`.
- Cadastrar chaves SSH no Jenkins (`host-1-ssh`, `host-2-ssh`, …).
- Implementar fetch/cópia de logs nos playbooks.
- Validar build end-to-end.
