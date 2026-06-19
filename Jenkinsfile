def logSourcePath = '/var/log/app/*.log'

properties([
    parameters([
        choice(
            name: 'LOG_PERIOD',
            choices: ['TODAY', 'YESTERDAY', 'TOTAL'],
            description: 'Período de logs: hoje, ontem ou total disponível'
        ),
    ]),
])

node('ansible-pod') {

    // pwd() após checkout é mais confiável que env.WORKSPACE em agentes customizados
    def projectDir = ''

    stage('Sincronizar repositório') {
        checkout scm
        sh 'git pull origin "${BRANCH_NAME:-main}" || true'
        projectDir = pwd()
        echo "Diretório do projeto: ${projectDir}"
    }

    stage('Resolver período') {
        def resolver = load "${projectDir}/scripts/resolve-log-period.groovy"
        def resolved = resolver(params.LOG_PERIOD)

        env.LOG_PERIOD = resolved.logPeriod
        env.LOG_DATE = resolved.logDate ?: ''

        echo "LOG_PERIOD=${env.LOG_PERIOD} LOG_DATE=${env.LOG_DATE ?: '(n/a para total)'}"
    }

    stage('Testar conexão') {
        def inventory = readYaml file: "${projectDir}/job/inventories/hosts.yaml"
        def loadInventory = load "${projectDir}/scripts/load-inventory.groovy"
        def hosts = loadInventory(inventory.all.hosts)

        hosts.each { host ->
            echo "Teste SSH ${host.name} → credencial: ${host.credentialId}"

            sshagent([host.credentialId]) {
                sh """
                    set -e
                    cd '${projectDir}'
                    ansible-playbook \\
                      -i job/inventories/hosts.yaml \\
                      job/playbooks/playbook-test-connectivity.yaml \\
                      --limit ${host.name}
                """
            }
        }
    }

    stage('Executar playbooks') {
        def inventory = readYaml file: "${projectDir}/job/inventories/hosts.yaml"
        def loadInventory = load "${projectDir}/scripts/load-inventory.groovy"
        def hosts = loadInventory(inventory.all.hosts)

        hosts.each { host ->
            def playbookPath = "job/playbooks/playbook-${host.name}.yaml"

            echo "Host ${host.name} → credencial Jenkins: ${host.credentialId}"

            sshagent([host.credentialId]) {
                sh """
                    set -e
                    cd '${projectDir}'
                    test -f ${playbookPath} || { echo "Playbook não encontrado: ${playbookPath}"; exit 1; }
                    ansible-playbook \\
                      -i job/inventories/hosts.yaml \\
                      ${playbookPath} \\
                      --limit ${host.name} \\
                      -e "log_period=${env.LOG_PERIOD} log_date=${env.LOG_DATE} log_source_path=${logSourcePath} workspace_dir=${projectDir}"
                """
            }
        }
    }

    stage('Publicar artefatos') {
        dir(projectDir) {
            archiveArtifacts artifacts: 'artifacts/**/*', allowEmptyArchive: true
        }
    }
}
