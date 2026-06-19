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

    stage('Sincronizar repositório') {
        checkout scm
        sh 'git pull origin "${BRANCH_NAME:-main}" || true'
    }

    stage('Resolver período') {
        def resolver = load "${env.WORKSPACE}/scripts/resolve-log-period.groovy"
        def resolved = resolver(params.LOG_PERIOD)

        env.LOG_PERIOD = resolved.logPeriod
        env.LOG_DATE = resolved.logDate ?: ''

        echo "LOG_PERIOD=${env.LOG_PERIOD} LOG_DATE=${env.LOG_DATE ?: '(n/a para total)'}"
    }

    stage('Testar conexão') {
        def inventory = readYaml file: "${env.WORKSPACE}/job/inventories/hosts.yaml"
        def loadInventory = load "${env.WORKSPACE}/scripts/load-inventory.groovy"
        def hosts = loadInventory(inventory.all.hosts)
        def testPlaybook = 'job/playbooks/playbook-test-connectivity.yaml'

        hosts.each { host ->
            echo "Teste SSH ${host.name} → credencial: ${host.credentialId}"

            sshagent([host.credentialId]) {
                sh """
                    set -e
                    ansible-playbook \\
                      -i job/inventories/hosts.yaml \\
                      ${testPlaybook} \\
                      --limit ${host.name}
                """
            }
        }
    }

    stage('Executar playbooks') {
        def inventory = readYaml file: "${env.WORKSPACE}/job/inventories/hosts.yaml"
        def loadInventory = load "${env.WORKSPACE}/scripts/load-inventory.groovy"
        def hosts = loadInventory(inventory.all.hosts)

        hosts.each { host ->
            def playbookPath = "job/playbooks/playbook-${host.name}.yaml"

            echo "Host ${host.name} → credencial Jenkins: ${host.credentialId}"

            sshagent([host.credentialId]) {
                sh """
                    set -e
                    test -f ${playbookPath} || { echo "Playbook não encontrado: ${playbookPath}"; exit 1; }
                    ansible-playbook \\
                      -i job/inventories/hosts.yaml \\
                      ${playbookPath} \\
                      --limit ${host.name} \\
                      -e "log_period=${env.LOG_PERIOD} log_date=${env.LOG_DATE} log_source_path=${logSourcePath}"
                """
            }
        }
    }

    stage('Publicar artefatos') {
        archiveArtifacts artifacts: 'artifacts/**/*', allowEmptyArchive: true
    }
}
