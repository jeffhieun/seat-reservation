import { test, expect } from '@playwright/test';
import { registerUser, loginUser } from './helpers/authHelper';
import { navigateToSeats, getAvailableSeat, reserveSeat, getReservationFromTable } from './helpers/reservationHelper';

test.describe('Reservation E2E', () => {
  const testEmail = `reservation-test-${Date.now()}@example.com`;
  const testPassword = 'TestPassword123';

  test.beforeEach(async ({ page }) => {
    // Clear storage and register/login
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
    
    await registerUser(page, testEmail, testPassword);
    await page.evaluate(() => localStorage.clear());
    await loginUser(page, testEmail, testPassword);
  });

  test('should view available seats', async ({ page }) => {
    // Navigate to seats page
    await navigateToSeats(page);
    
    // Verify seats are displayed
    const seats = await page.locator('.seat-card').all();
    expect(seats.length).toBeGreaterThan(0);
    
    // Verify at least one available seat
    let hasAvailableSeat = false;
    for (const seat of seats) {
      const status = await seat.locator('.seat-card-status').textContent();
      if (status?.includes('AVAILABLE')) {
        hasAvailableSeat = true;
        break;
      }
    }
    expect(hasAvailableSeat).toBe(true);
  });

  test('should reserve a seat successfully', async ({ page }) => {
    // Navigate to seats
    await navigateToSeats(page);
    
    // Get first available seat
    const availableSeat = await getAvailableSeat(page);
    expect(availableSeat).not.toBeNull();
    
    if (!availableSeat) return;
    
    // Reserve the seat
    await availableSeat.element.click();
    
    // Wait for either success message or navigation
    await page.waitForSelector('.success-text, .error-text', { timeout: 5000 }).catch(() => {
      // May navigate directly
    });
    
    // Check if reservation was created in the table
    await page.waitForSelector('table tbody tr', { timeout: 5000 }).catch(() => {
      // Table might be loading
    });
    
    // Verify in reservations table
    const reservation = await getReservationFromTable(page, availableSeat.seatNumber);
    expect(reservation).not.toBeNull();
    
    if (reservation && reservation.cells.length > 1) {
      const status = await reservation.cells[1].textContent();
      expect(status).toContain('PENDING_PAYMENT');
    }
  });

  test('should display reservation in reservation list', async ({ page }) => {
    // Navigate to seats
    await navigateToSeats(page);
    
    // Get and reserve a seat
    const availableSeat = await getAvailableSeat(page);
    expect(availableSeat).not.toBeNull();
    
    if (!availableSeat) return;
    
    const seatNumber = availableSeat.seatNumber;
    await availableSeat.element.click();
    
    // Wait for table to update
    await page.waitForSelector('table tbody tr', { timeout: 5000 }).catch(() => {});
    
    // Scroll to reservations section if needed
    const reservationTable = await page.$('table');
    if (reservationTable) {
      await reservationTable.scrollIntoViewIfNeeded();
    }
    
    // Verify reservation appears in table
    const reservation = await getReservationFromTable(page, seatNumber);
    expect(reservation).not.toBeNull();
  });

  test('should show error when attempting duplicate reservation', async ({ page }) => {
    // Navigate to seats
    await navigateToSeats(page);
    
    // Get first available seat
    const availableSeat = await getAvailableSeat(page);
    expect(availableSeat).not.toBeNull();
    
    if (!availableSeat) return;
    
    // Reserve the seat
    await availableSeat.element.click();
    
    // Wait for success or table update
    await page.waitForSelector('table tbody tr, .success-text', { timeout: 5000 }).catch(() => {});
    
    // Try to reserve same seat again
    await availableSeat.element.click();
    
    // Should show error or conflict message
    await page.waitForSelector('.error-text', { timeout: 5000 });
    const errorText = await page.textContent('.error-text');
    expect(errorText).toBeTruthy();
    expect(errorText?.toLowerCase()).toMatch(/already|duplicate|conflict/);
  });

  test('should display seat status changes correctly', async ({ page }) => {
    // Navigate to seats
    await navigateToSeats(page);
    
    // Get first available seat
    const availableSeat = await getAvailableSeat(page);
    expect(availableSeat).not.toBeNull();
    
    if (!availableSeat) return;
    
    const initialStatus = await availableSeat.element.locator('.seat-card-status').textContent();
    expect(initialStatus).toContain('AVAILABLE');
    
    // Reserve the seat
    await availableSeat.element.click();
    
    // Wait for UI to update
    await page.waitForTimeout(1000);
    
    // Refresh to get updated seat status
    const seatCards = await page.locator('.seat-card').all();
    let seatUpdated = false;
    
    for (const card of seatCards) {
      const cardNumber = await card.locator('.seat-card-number').textContent();
      if (cardNumber === availableSeat.seatNumber) {
        const newStatus = await card.locator('.seat-card-status').textContent();
        // Status should change to pending or reserved
        if (newStatus?.includes('PENDING') || newStatus?.includes('RESERVED')) {
          seatUpdated = true;
        }
        break;
      }
    }
    
    // Seat status should change
    expect(seatUpdated).toBe(true);
  });

  test('should navigate to reservation details from table', async ({ page }) => {
    // Navigate to seats and make a reservation
    await navigateToSeats(page);
    
    const availableSeat = await getAvailableSeat(page);
    expect(availableSeat).not.toBeNull();
    
    if (!availableSeat) return;
    
    await availableSeat.element.click();
    
    // Wait for table to appear
    await page.waitForSelector('table tbody tr', { timeout: 5000 }).catch(() => {});
    
    // Get reservation row and click view/confirm button if present
    const reservation = await getReservationFromTable(page, availableSeat.seatNumber);
    
    if (reservation) {
      // Look for a link or button in the reservation row
      const links = await reservation.element.locator('a').all();
      if (links.length > 0) {
        await links[0].click();
        // Should navigate to reservation or payment page
        await page.waitForURL(/\/(reservations|payment)\/\d+/, { timeout: 5000 });
        expect(page.url()).toMatch(/\/(reservations|payment)\/\d+/);
      }
    }
  });
});
