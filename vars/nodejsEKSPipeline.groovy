def call(Map configMap) {
    pipeline {

        agent {
            label agent-1
        }

        stages{
            stage('cloning git repo'){
                steps{

                }
            }

            stage('install dependencies'){
                steps{
                    sh"""
                       npm install 
                    """
                }
            }
        }

    }
}