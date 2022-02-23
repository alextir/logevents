# Getting Started

### build proj
- exec command into proj dir exec: `mvn clean install`

### run proj
- easiest from ide with default params in app.properties or navigate to target dir and exec:
- `java -jar logevents-0.0.1-SNAPSHOT.jar --logfile.path=file:<ABSOLUTE_PATH_LOGFILE_TXT> --spring.datasource.url=jdbc:hsqldb:file:<DBTEST_FILE>`  
- additional params for tuning:
  `pool.size.min` - num of core consumer (writers) threads
  `pool.size.max` - num of max consumer (writers) threads
