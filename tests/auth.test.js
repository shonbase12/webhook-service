// Example test for authentication logic

const request = require('supertest');
const app = require('../app'); // Adjust the path to your app

describe('DELETE /deactivate', () => {
    it('should require authentication', async () => {
        const response = await request(app)
            .delete('/deactivate')
            .send();
        expect(response.status).toBe(401); // Unauthorized
    });

    it('should allow authenticated users to deactivate', async () => {
        const response = await request(app)
            .delete('/deactivate')
            .set('Authorization', 'Bearer VALID_TOKEN') // Replace with a valid token
            .send();
        expect(response.status).toBe(200); // Success
    });
});