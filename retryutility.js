// Sample code for retry utility
function retry(operation, retries) {
  for (let i = 0; i < retries; i++) {
    try {
      return operation();
    } catch (e) {
      console.log(`Retrying... ${i + 1}/${retries}`);
    }
  }
  throw new Error('Operation failed');
}

module.exports = { retry };