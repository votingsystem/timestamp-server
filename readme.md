## Config files

The application expects the content of the distribution directory 'docs/app-config-files' with values appropriate to the 
installation in the directory:

    /var/local/timestamp-server

It can be changed setting system environment var 'timestamp_server_dir' with the desired value

### Configuration
 - The application has been developed and tested on Wildfly 10.
 - The application default working dir is in **/var/local/timestamp-server**
you can modify that location changing the system property **timestamp_server_dir**
 - Inside the config dir there must the content of **docs/config** completed with the values of your installation   

#### enable appplication filter
To enable application filters change what follows to the standalone / domain  server configuration 
([details] (https://docs.jboss.org/author/display/WFLY8/Undertow+subsystem+configuration)):
    
        <servlet-container name="default" allow-non-standard-wrappers="true">

#### Build and Deploy on Wildfly
1. Run the script **setup-database-postgres.sh** in order to create the database (it must be in the same folder that **timestamp-server-postgres.sql**)
2. Make sure you have  Wildfly server started.
3. Make sure theres configured a _datasource_ with the name:

    _java:jboss/datasources/timestamp-server_
        
4. Use this command to build and deploy the archive:

            mvn clean package wildfly:deploy
      
#### Logging
Application will store log files in directory:
    
    /var/log/timestamp-server
    
You must setup the folder with the appropiated user privileges 

#### Certificates
Certificate dates are expressed as UTC time (Coordinated Universal Time) 
to reduce confusion with the local time zone use. Comparison are made to the millisecond.

**genTime** is the time at which the time-stamp token has been created by
the TSA.  It is expressed as UTC time (Coordinated Universal Time) to
reduce confusion with the local time zone use. 