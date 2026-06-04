// New validation logic
export function validateRequest(data) {
  if (!data || !data.payload) {
    throw new Error('Invalid request payload');
  }
  return true;
}