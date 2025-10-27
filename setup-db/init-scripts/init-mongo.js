// Script d'initialisation MongoDB (exécuté au premier démarrage)

db = db.getSiblingDB('blogpress');

// Créer un utilisateur applicatif (optionnel)
db.createUser({
  user: 'blogpress_user',
  pwd: 'blogpress_password',
  roles: [
    {
      role: 'readWrite',
      db: 'blogpress'
    }
  ]
});

// Créer les collections avec validation (optionnel)
db.createCollection('users');
db.createCollection('blogs');
db.createCollection('articles');
db.createCollection('likes');
db.createCollection('favorites');
db.createCollection('follows');

print('✅ MongoDB initialized successfully');