import { test, expect } from '@playwright/test';
import { registerUser, loginUser, logout, isUserAuthenticated } from './helpers/authHelper';

test.describe('Authentication E2E', () => {
  const testEmail = `test-${Date.now()}@example.com`;
  const testPassword = 'TestPassword123';
  
  test.beforeEach(async ({ page }) => {
    // Clear storage before each test
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
  });

  test('should register a new user successfully', async ({ page }) => {
    // Register new user
    await registerUser(page, testEmail, testPassword);
    
    // Verify redirected to login page
    expect(page.url()).toContain('/login');
  });

  test('should login successfully with valid credentials', async ({ page }) => {
    // First register a user
    await registerUser(page, testEmail, testPassword);
    
    // Clear storage to simulate fresh login
    await page.evaluate(() => localStorage.clear());
    
    // Login with valid credentials
    await loginUser(page, testEmail, testPassword);
    
    // Verify authenticated and on seats page
    expect(page.url()).toContain('/seats');
    const isAuthenticated = await isUserAuthenticated(page);
    expect(isAuthenticated).toBe(true);
    
    // Verify user email displayed in navbar
    const navbarUser = await page.textContent('.navbar-user');
    expect(navbarUser).toContain(testEmail);
  });

  test('should show error on invalid login', async ({ page }) => {
    await page.goto('/login');
    
    // Try to login with non-existent user
    await page.fill('#email', 'nonexistent@example.com');
    await page.fill('#password', 'wrongpassword');
    await page.click('button[type="submit"]');
    
    // Verify error message displayed
    const errorText = await page.textContent('.error-text');
    expect(errorText).toBeTruthy();
    
    // Should remain on login page
    expect(page.url()).toContain('/login');
  });

  test('should show error when password is too short', async ({ page }) => {
    await page.goto('/auth/register');
    
    // Try to register with short password
    await page.fill('#email', testEmail);
    await page.fill('#password', 'short');
    await page.fill('#confirmPassword', 'short');
    
    const submitButton = await page.$('button[type="submit"]');
    // Button should be disabled or show validation error
    const isDisabled = await submitButton?.isDisabled();
    
    if (!isDisabled) {
      await page.click('button[type="submit"]');
      const errorText = await page.textContent('.error-text');
      expect(errorText).toContain('at least 6 characters');
    }
  });

  test('should show error when passwords do not match', async ({ page }) => {
    await page.goto('/auth/register');
    
    // Register with mismatched passwords
    await page.fill('#email', testEmail);
    await page.fill('#password', testPassword);
    await page.fill('#confirmPassword', 'DifferentPassword123');
    await page.click('button[type="submit"]');
    
    // Verify error message displayed
    const errorText = await page.textContent('.error-text');
    expect(errorText).toContain('match');
  });

  test('should logout successfully', async ({ page }) => {
    // First login
    await registerUser(page, testEmail, testPassword);
    await page.evaluate(() => localStorage.clear());
    await loginUser(page, testEmail, testPassword);
    
    // Verify logged in
    let isAuthenticated = await isUserAuthenticated(page);
    expect(isAuthenticated).toBe(true);
    
    // Logout
    await logout(page);
    
    // Verify logged out
    isAuthenticated = await isUserAuthenticated(page);
    expect(isAuthenticated).toBe(false);
    expect(page.url()).toContain('/login');
  });

  test('should redirect to login when accessing protected route without token', async ({ page }) => {
    // Try to access protected route without token
    await page.goto('/seats');
    
    // Should redirect to login
    expect(page.url()).toContain('/login');
  });

  test('should show error when email is already registered', async ({ page }) => {
    // First register a user
    await registerUser(page, testEmail, testPassword);
    
    // Navigate back to register
    await page.goto('/auth/register');
    
    // Try to register with same email
    await page.fill('#email', testEmail);
    await page.fill('#password', testPassword);
    await page.fill('#confirmPassword', testPassword);
    await page.click('button[type="submit"]');
    
    // Wait for error message
    await page.waitForSelector('.error-text', { timeout: 5000 });
    const errorText = await page.textContent('.error-text');
    expect(errorText).toBeTruthy();
  });
});
