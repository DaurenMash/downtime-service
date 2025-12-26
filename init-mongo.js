db = db.getSiblingDB('downtime_db');

db.createUser({
    user: 'downtime_user',
    pwd: 'downtime_pass',
    roles: [
        {
            role: 'readWrite',
            db: 'downtime_db'
        }
    ]
});

db.createCollection('downtime_events');
db.downtime_events.createIndex({ equipmentId: 1 });
db.downtime_events.createIndex({ status: 1 });
db.downtime_events.createIndex({ startTime: 1 });
db.downtime_events.createIndex({ operatorId: 1 });