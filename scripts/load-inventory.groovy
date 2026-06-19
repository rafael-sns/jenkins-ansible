/**
 * Valida e normaliza hosts do inventário Ansible para uso no Jenkins.
 *
 * Espera em cada host:
 *   - jenkins_ssh_credential: ID da credencial SSH (chave) cadastrada no Jenkins
 *   - ansible_user (opcional): usuário SSH — também lido pelo Ansible via inventário
 *
 * @param hosts mapa all.hosts do hosts.yaml
 * @return lista de maps [name, credentialId, user]
 */
def call(Map hosts) {
    if (!hosts) {
        error('Inventário vazio: nenhum host em all.hosts')
    }

    return hosts.collect { name, vars ->
        def credId = vars?.jenkins_ssh_credential
        if (!credId) {
            error("Host '${name}' sem 'jenkins_ssh_credential' em job/inventories/hosts.yaml")
        }

        [
            name        : name.toString(),
            credentialId: credId.toString(),
            user        : vars?.ansible_user?.toString(),
        ]
    }
}

return this
