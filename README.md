# sendsafely-upload-tool

Instructions:

Build the jar with dependencies using maven:
<mvn clean compile assembly:single>

Run the jar:
<java -jar target/upload-cli-1.0-SNAPSHOT-jar-with-dependencies.jar SendSafelyHost UserApiKey UserApiSecret -d:--debug>
