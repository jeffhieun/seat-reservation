/**
 * Payment helper functions for E2E tests
 */

export async function initiatePayment(page, reservationId) {
  // Navigate to payment page
  await page.goto(`/payment/${reservationId}`);
  
  // Wait for payment page to load
  await page.waitForSelector('.details-card');
  
  // Click Pay button
  await page.click('button:has-text("Pay")');
  
  // Wait for navigation to payment processing page
  await page.waitForURL(/\/payment\/processing\/\d+/);
  
  // Extract payment ID from URL
  const url = page.url();
  const paymentMatch = url.match(/\/payment\/processing\/(\d+)/);
  return paymentMatch ? paymentMatch[1] : null;
}

export async function completePaymentSuccessInDev(page, paymentId) {
  // Call the dev endpoint to mark payment as successful
  await page.goto(`/payment/${paymentId}/success`);
}

export async function completePaymentFailureInDev(page, paymentId) {
  // Call the dev endpoint to mark payment as failed
  await page.goto(`/payment/${paymentId}/failure`);
}

export async function waitForPaymentSuccess(page, timeout = 60000) {
  // Wait for navigation to success page
  await page.waitForURL('/payment/success', { timeout });
}

export async function waitForPaymentFailure(page, timeout = 60000) {
  // Wait for navigation to failed page
  await page.waitForURL('/payment/failed', { timeout });
}

export async function getPaymentReferenceFromPage(page) {
  // Look for payment reference in page content
  const content = await page.textContent('body');
  const match = content.match(/PAY_[A-F0-9]+/);
  return match ? match[0] : null;
}

export async function getReservationIdFromUrl(page) {
  const url = page.url();
  const match = url.match(/\/payment\/(\d+)/);
  return match ? match[1] : null;
}
