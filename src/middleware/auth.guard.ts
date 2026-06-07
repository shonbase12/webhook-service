function authGuard(req, res, next) {
  // Add authentication logic here
  next();
}

module.exports = authGuard;