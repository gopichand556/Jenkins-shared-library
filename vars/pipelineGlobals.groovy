def getAccountID(String environment){
    switch(environment) { 
        case 'dev': 
            return "337909750491"
        case 'qa':
            return "337909750491"
        case 'uat':
            return "337909750491"
        case 'pre-prod':
            return "337909750491"
        case 'prod':
            return "337909750491"
        default:
            return "nothing"
    } 
}