/**
 * Reservation helper functions for E2E tests
 */

export async function navigateToSeats(page) {
  await page.goto('/seats');
  // Wait for seats to load
  await page.waitForSelector('.seat-card');
}

export async function getAvailableSeat(page) {
  // Get the first available seat
  const seats = await page.locator('.seat-card').all();
  
  for (const seat of seats) {
    const status = await seat.locator('.seat-card-status').textContent();
    if (status?.includes('AVAILABLE')) {
      const seatNumber = await seat.locator('.seat-card-number').textContent();
      return { element: seat, seatNumber };
    }
  }
  
  return null;
}

export async function reserveSeat(page) {
  const availableSeat = await getAvailableSeat(page);
  
  if (!availableSeat) {
    throw new Error('No available seats found');
  }
  
  // Click the seat card to reserve
  await availableSeat.element.click();
  
  // Wait for success message or confirmation
  await page.waitForSelector('.success-text, .error-text', { timeout: 5000 }).catch(() => {
    // May not have success text, check if navigated to reservation
  });
  
  return availableSeat.seatNumber;
}

export async function getReservationFromTable(page, seatNumber) {
  const rows = await page.locator('table tbody tr').all();
  
  for (const row of rows) {
    const cells = await row.locator('td').all();
    if (cells.length > 0) {
      const cellText = await cells[0].textContent();
      if (cellText?.includes(seatNumber)) {
        return {
          element: row,
          cells: cells,
        };
      }
    }
  }
  
  return null;
}

export async function waitForReservationStatus(page, seatNumber, expectedStatus, timeout = 10000) {
  const startTime = Date.now();
  
  while (Date.now() - startTime < timeout) {
    const reservation = await getReservationFromTable(page, seatNumber);
    
    if (reservation && reservation.cells.length > 1) {
      const status = await reservation.cells[1].textContent();
      if (status?.includes(expectedStatus)) {
        return reservation;
      }
    }
    
    // Refresh if available
    await page.evaluate(() => window.location.reload());
    await page.waitForSelector('table tbody tr', { timeout: 5000 }).catch(() => {});
    
    // Small wait before retry
    await page.waitForTimeout(1000);
  }
  
  throw new Error(`Reservation status did not change to ${expectedStatus} within ${timeout}ms`);
}
