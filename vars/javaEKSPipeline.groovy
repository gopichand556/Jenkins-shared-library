def call(Map configMap) {
    pipeline {

        agent {
            label agent-1
        }
        options{
            timeout(time: 30, unit: 'MINUTES')
            disableConcurrentBuilds()
            //retry(1)
        }

    parameters {
        booleanParam(
            name: 'deploy', 
            defaultValue: false, 
            description: 'Select deploy or not'
        )
    }

        environment {
            project = configMap.get('project')
            componenet = configMap.get('component')
            region = 'us-east-1'
            enviroment = 'dev'
            account_id = pipelineGlobals.getAccountID(environment)
            appVersion = ''
            
        }

        stages{
            stage('cloning git repo'){
                steps{
                      
                       git url: "https://github.com/${env.GIT_URL}.git", branch: "${env.GIT_BRANCH}"

                }
            }

            stages {
            stage('Read the version') {
                steps {
                    script{
                        def pom = readMavenPom file: 'pom.xml'
                        appVersion = pom.version
                        echo "App version: ${appVersion}"
                    }
                }
            }
            stage('Install Dependencies') {
                steps {
                    sh 'mvn clean package'
                }
            }

            stage('scan dependencies') {
                steps{

                }
            }

            stage('SonarQube analysis') {
                environment {
                    SCANNER_HOME = tool 'sonar-6.0' //scanner config
                }
                steps {
                    // sonar server injection
                    withSonarQubeEnv('sonar-6.0') {
                        sh '$SCANNER_HOME/bin/sonar-scanner'
                        //generic scanner, it automatically understands the language and provide scan results
                    }
                }
            }

            stage('Quality Gate') {
                steps {
                    timeout(time: 5, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }

            stage('Docker build') {
                
                steps {
                    withAWS(region: 'us-east-1', credentials: "aws-creds-${environment}") {
                        sh """
                        aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.us-east-1.amazonaws.com

                        docker build -t ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${environment}/${component}:${appVersion} .

                        docker images

                        """
                    }
                }
            }

            stage('scan the image'){
                steps{

                }
            }

            stage('push the image'){
                steps{
                      

                      withAWS(region: 'us-east-1', credentials: "aws-creds-${environment}") {
                        sh """

                        docker push ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${environment}/${component}:${appVersion}
                        """
                }
            }
        }

         stage('Deploy'){
                when{
                    expression {params.deploy}
                }

                steps{
                    build job: "../${component}-cd", parameters: [
                        string(name: 'VERSION', value: "$appVersion"),
                        string(name: 'ENVIRONMENT', value: "dev"),
                    ], wait: true
                }
            }
        }

        post {
        always{
            deleteDir()
        }
        success {

        }

        failure{

        }
    }


    
    }
}