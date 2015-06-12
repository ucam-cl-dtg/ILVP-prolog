
# Install Postgresql and Tomcat

sudo apt-get install postgresql libpg-java tomcat7

# Configure Postgresql and initialise database

sudo mkdir /local/data/postgres
sudo chown postgres:postgres /local/data/postgres/
echo | sudo su postgres -c "psql -d template1" <<EOF
create tablespace data location '/local/data/postgres';
create database lectures with encoding='UTF-8' tablespace data;
create user lectures with password 'lectures';
grant all privileges on database lectures to lectures;
EOF
psql -U lectures -f db.sql lectures

# Configure Tomcat to use Postgres and initialise support for skinny war

Add the following to /etc/tomcat7/context.xml:
 <Resource auth="Container" driverClassName="org.postgresql.Driver"
  maxActive="8" maxIdle="4" name="jdbc/lectures" password="lectures"
  type="javax.sql.DataSource" url="jdbc:postgresql://localhost/lectures"
 username="lectures" validationQuery="select 1" />
sudo ln -s /usr/share/java/postgresql.jar /usr/share/tomcat7/lib/
sudo ufw allow proto tcp from any to any port 80
#edit /etc/default/tomcat7  and ensure that AUTHBIND=yes is set to allow service on port 80
#edit /etc/tomcat7/server.xml and change the server port from 8080 to 80
#By default the project pom.xml builds a skinny jar, so two additional jars are required:
cp commons-io-2.4.jar soy-1.0.jar /usr/share/tomcat7/lib/
sudo apt-get install liblog4j1.2-java libcommons-codec-java libcommons-logging-java
cd /usr/share/tomcat7/lib/ 
sudo ln -s ../../java/commons-codec.jar
sudo ln -s ../../java/log4j-1.2.jar
sudo ln -s ../../java/commons-logging.jar
sudo service tomcat7 start 

# Insert video in the correct place

sudo mkdir /local/data/video
sudo ln -s /local/data/video /var/lib/tomcat7/webapps/
#install the videos in /local/data/video

# Install lecture server software

sudo cp ls.war /var/lib/tomcat7/webapps/

# Visit the website

firefox http://prolog.cl.cam.ac.uk/ls/teaching/1415/ 
