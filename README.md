# trial-account-handler(WSO2-IS)

This custom event handler is based on the [WSO2-Idle-Account-handler](https://github.com/wso2-extensions/identity-governance/tree/master/components/org.wso2.carbon.identity.account.suspension.notification.task).This handler
is written to achive the following,
* Introduce the concept of trial(temporary) accounts to WSO2 Identity Server.
* Define a trial period for trial accounts.
* Mark the trial accounts as expired when the trial period ends.
* Prevent trial users from authenticating if the trial periods has expired.

## Instructions to add the component to the IS
1. Add the following local claims to IS
   1. `http://wso2.org/claims/identity/trialAccountExpired`
   2. `http://wso2.org/claims/identity/trialAccount`
2. Aquire the files and build the handler using `mvn clean install`
3. Place the built jar file inside _<IS_HOME>/repository/components/dropins directory_
4. Add the following configurations to _<IS_HOME>/repository/conf/identity/identity-event.properties_ file
```java
module.name.13=trialAccountHandler
trialAccountHandler.subscription.1=PRE_AUTHENTICATION
trialAccountHandler.trial.account.expiry.enabled=true
trialAccountHandler.trial.account.period=30
trialAccountHandler.trial.account.expiry.trigger.time=15:00:00
```
Configuration | Value
------------ | -------------
module.name.13 | The module number can vary depending on the number of modules you have configured therefore make sure to add one value to the last module and register the `trialAccountHandler`
trialAccountHandler.subscription.1 | The trial account handler will intercept all `PRE_AUTHENTICATION` events only
trialAccountHandler.trial.account.expiry.enabled | To enable the trial account handler set the value to `true`.
trialAccountHandler.trial.account.period | Trial account period in days
trialAccountHandler.trial.account.expiry.trigger.time | The local time of the day which should initiate the trial account susspention task. Choose a time which has less amount of load on the IS
5. Restart the Server.



  
