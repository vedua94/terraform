pipeline { 
  agent any
  options {
        ansiColor('xterm')
  }
  parameters {
    choice(name: 'ENV', choices: ['sb','pr', 'pp'], description: 'Select the environment to deploy into.')
    choice(name: 'TYPE', choices: ['cluster', 'customer'], description: 'Select whether this is a cluster deployment or a customer deployment.')
    string(name: 'CLUSTERNAME', defaultValue: '', description: 'Name of the cluster to run')
    string(name: 'CUSTOMERNAME', defaultValue: '', description: 'Name of the customer to run')
    choice(name: 'ACTION', choices: ['plan', 'apply', 'destroy'], description: 'Run terraform plan or terraform apply')
  }
  stages {
        stage('copy_credential') {
            steps {
                withCredentials([file(credentialsId: "gcloud_cred", variable: 'GC_KEY')]) {
                    sh """
                    cp $GC_KEY ${WORKSPACE}/cred.json
                    """
                }   
            }
        }
        stage('terraform apply/destroy') {
            steps {
                script{
                if (params.ACTION =='apply') 
                sh """
                terraform init 
                terraform plan -out=plan.tfplan
                """
                input(message: 'Click "proceed" to approve the above Terraform plan')
                sh """
                terraform apply --auto-approve
                """
                if (params.ACTION =='destroy')
                sh """
                terraform init 
                terraform plan -out=plan.tfplan
                terraform destroy --auto-approve
                """
                else
                echo "Nothing to change"
                }
            }
        }
   }
   post {
        always {
            echo '###### cleaning WorkSpace #######'
            cleanWs notFailBuild: true, patterns: [[pattern: '${WORKSPACE}/creds.json', type: 'INCLUDE']]
        }
    }    
}

