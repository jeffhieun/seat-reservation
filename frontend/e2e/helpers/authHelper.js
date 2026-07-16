/**
 * Authentication helper functions for E2E tests
 */

export async function registerUser(page, email, password) {
  await page.goto('/auth/register');
  await page.fill('#email', email);
  await page.fill('#password', password);
  await page.fill('#confirmPassword', password);
  await page.click('button[type="submit"]');
  
  // Wait for success message and redirect to login
  await page.waitForURL('/login');
}

export async function loginUser(page, email, password) {
  await page.goto('/login');
  await page.fill('#email', email);
  await page.fill('#password', password);
  await page.click('button[type="submit"]');
  
  // Wait for navigation to seats page
  await page.waitForURL('/seats');
}

export async function logout(page) {
  await page.click('.navbar-logout');
  await page.waitForURL('/login');
}

export async function getStoredToken(page) {
  return await page.evaluate(() => localStorage.getItem('token'));
}

export async function isUserAuthenticated(page) {
  const token = await getStoredToken(page);
  return !!token;
}
