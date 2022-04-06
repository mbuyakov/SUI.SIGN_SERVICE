pipeline {
    options {
        buildDiscarder logRotator(numToKeepStr: '3')
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam(defaultValue: false, description: 'Clean workspace', name: 'clean_ws')
    }

    environment {
        SUFFIX = "${env.BRANCH_NAME == "master" ? " " : ("-" + env.BRANCH_NAME)}"
    }

    agent any

    stages {
        stage('Build and put to registry') {
            environment {
                NEXUS = credentials('suilib-nexus')
            }
            steps {
                sh '''
                    cd ${WORKSPACE}
                    docker build -t nexus.suilib.ru:10401/repository/docker-sui/sui-sign-service:${BUILD_NUMBER}${SUFFIX} .
                    docker login nexus.suilib.ru:10401/repository/docker-sui/ --username ${NEXUS_USR} --password ${NEXUS_PSW}
                    docker push nexus.suilib.ru:10401/repository/docker-sui/sui-sign-service:${BUILD_NUMBER}${SUFFIX}
                '''
            }
            post {
                always {
                    sh """
                        docker logout nexus.suilib.ru:10401/repository/docker-sui/
                    """
                }
            }
        }
        stage('Clean workspace') {
            when {
                environment name: 'clean_ws', value: 'true'
            }
            steps {
                cleanWs()
            }
        }
    }
}