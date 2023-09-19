## Module Layout
The wallet API repository consists of single module:

```wallet``` Spring boot application which hosts the Wallet REST APIs.

## Build
Build the whole solution by running the following command from the repository root: 
```mvn clean package```

## Run
java -Xms8192m -Xmx8192m -jar /u01/deployment/wallet/Wallet-0.0.1-SNAPSHOT.jar -Dspring.profiles.active=dev

**Remote Deployment**
```
export spring_profiles_active=test
nohup java -Xms8192m -Xmx8192m -jar /u01/deployment/wallet/Wallet-0.0.1-SNAPSHOT.jar > /tmp/wallet.log 2>&1 &
```

**Profiles**
```
dev ==> development
test ==> Test & Development
prod ==> Production
```
