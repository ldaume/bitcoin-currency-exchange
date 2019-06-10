if (db._databases().indexOf('bitcoin') < 0) {
  db._createDatabase("bitcoin");
}

db._useDatabase('bitcoin');

if (!db.rates){
  db._create('rates');
}


if (!db.users){
  db._create('users');
}

db._useDatabase('_system');

try { require('@arangodb/users').save('test', 'test'); } catch (e) {}

try { require('@arangodb/users').grantDatabase('test', 'bitcoin', 'rw'); } catch (e) {}