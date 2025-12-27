docker-compose down
docker-compose build downtime-service
docker-compose up -d



@REM docker exec -it downtime-mongo bash
@REM mongosh -u admin -p password
@REM
@REM show dbs
@REM
@REM use downtime_db
@REM
@REM db.downtime_events.findOne()