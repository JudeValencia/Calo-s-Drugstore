# 🧪 Comprehensive Testing Guide for Bug Fixes

This guide provides step-by-step instructions to test all the bugs that were fixed in the system.

---

## 📋 Prerequisites

1. **Database Schema Update Required:**
   ```sql
   -- Run this SQL to add new columns to your database
   ALTER TABLE sale_items ADD COLUMN batch_info TEXT;
   ALTER TABLE sales ADD COLUMN voided BOOLEAN DEFAULT FALSE;
   ALTER TABLE sales ADD COLUMN void_date TIMESTAMP;
   ALTER TABLE sales ADD COLUMN void_reason VARCHAR(500);
   ```

2. **Start the Application:**
   - Run `DrugstoreApplication.java`
   - Login as ADMIN user (for full access)

---

## ✅ Bug #1: Stock Addition (Duplicate/Override Issue)

### What Was Fixed:
Products with the same name don't create duplicates when adding stock - they add to the existing product.

### Test Steps:

1. **Go to Inventory Module**
   - Click "Inventory" in the sidebar

2. **Add a New Product with Batch:**
   - Click "Add Product" button
   - Enter:
     - Medicine ID: `MED999`
     - Brand Name: `Test Medicine Alpha`
     - Stock: `50`
     - Price: `100.00`
     - Expiration: Select a future date
     - Supplier: `Test Supplier`
   - Click "Save"

3. **Try to Add Stock to Same Product:**
   - Click "Add Product" again
   - Enter THE SAME brand name: `Test Medicine Alpha`
   - Stock: `30`
   - Price: `100.00`
   - Expiration: Select a different future date
   - Supplier: `Test Supplier`
   - Click "Save"

### ✅ Expected Result:
- System should add a NEW BATCH to the existing product
- Product should now have 80 total stock (50 + 30)
- When you click "View Batches" on "Test Medicine Alpha", you should see 2 batches
- Should NOT create a duplicate product

### ❌ Failure Indicators:
- Two separate products named "Test Medicine Alpha" in the table
- Stock not combined

---

## ✅ Bug #2: Expiry Date Display When Stocks Depleted

### What Was Fixed:
When all stock is sold (stock = 0), the expiration date column shows "N/A" instead of showing the date.

### Test Steps:

1. **Create a Product with Batches:**
   - Add a product: `Test Expiry Product`
   - Stock: `5` units
   - Expiration: Tomorrow's date
   - Note the expiration date shown in the table

2. **Sell All Stock:**
   - Go to "Sales" module
   - Add `Test Expiry Product` to cart
   - Quantity: `5` (all stock)
   - Click "Complete Transaction"

3. **Check Inventory Table:**
   - Go back to "Inventory" module
   - Find `Test Expiry Product`
   - Look at the "Expiration" column

### ✅ Expected Result:
- Expiration column shows **"N/A"** (not the date)
- Stock column shows **0**

### ❌ Failure Indicators:
- Expiration date still displays even though stock is 0

---

## ✅ Bug #3: Perfect Batch Restoration (NEW FEATURE!)

### What Was Fixed:
When voiding/deleting transactions, stock now returns to the EXACT batches it came from, not all to one batch.

### Test Steps:

#### Part A: Setup Multiple Batches

1. **Create Product with First Batch:**
   - Go to Inventory
   - Add Product:
     - Name: `Restoration Test Med`
     - Stock: `200`
     - Expiration: Dec 20, 2025
     - Price: `50.00`
   - Click "Save"

2. **Add Second Batch:**
   - Click "Add Product" again
   - Name: `Restoration Test Med` (same name)
   - Stock: `1`
   - Expiration: Dec 25, 2025
   - Price: `50.00`
   - Click "Save"

3. **Verify Batches:**
   - Click "View Batches" button on the product
   - You should see:
     - Batch 1: 200 units (expires Dec 20)
     - Batch 2: 1 unit (expires Dec 25)
   - **Take a screenshot of this for comparison**

#### Part B: Sell All Stock

4. **Go to Sales Module:**
   - Click "Sales" in sidebar

5. **Create Transaction:**
   - Add `Restoration Test Med` to cart
   - Quantity: `201` (all stock)
   - Click "Complete Transaction"
   - Transaction should succeed

6. **Verify Stock Depleted:**
   - Go to Inventory
   - Click "View Batches" on `Restoration Test Med`
   - Both batches should show: **0 units**

#### Part C: Void Transaction

7. **Go to Reports Module:**
   - Click "Reports" in sidebar
   - Find the transaction you just created (should be at the top)

8. **Void the Transaction:**
   - Click the orange **"⊘ Void"** button
   - Enter a reason (optional): `Testing batch restoration`
   - Confirm the void

9. **Check Batch Restoration:**
   - Go back to Inventory
   - Click "View Batches" on `Restoration Test Med`

### ✅ Expected Result (PERFECT RESTORATION):
- **Batch 1 (Dec 20):** `200` units (exactly as before!)
- **Batch 2 (Dec 25):** `1` unit (exactly as before!)
- Total Stock: `201` units
- **Compare with your screenshot - should match perfectly!**

### ❌ Old Behavior (Before Fix):
- Batch 1: 201 units (all stock in one batch)
- Batch 2: 0 units

---

## ✅ Bug #4: Cart Data Visibility When Switching Modules

### What Was Fixed:
Cart items persist when navigating between modules - no need to re-add items.

### Test Steps:

1. **Go to Sales Module:**
   - Click "Sales" in sidebar

2. **Add Items to Cart:**
   - Add any product, quantity 2
   - Add another product, quantity 3
   - **DO NOT complete the transaction**
   - Cart should show 2 items

3. **Navigate Away:**
   - Click "Dashboard" in sidebar
   - Wait for dashboard to load

4. **Navigate to Inventory:**
   - Click "Inventory" in sidebar
   - Browse some products

5. **Return to Sales:**
   - Click "Sales" in sidebar

### ✅ Expected Result:
- Cart should **immediately show** the 2 items you added
- Items are visible without any action
- Quantities are correct

### ❌ Failure Indicators:
- Cart appears empty when returning to Sales
- Need to add another item to "trigger" the cart to show

---

## ✅ Bug #5: Void Transaction (Replace Delete)

### What Was Fixed:
Delete button replaced with Void button - transactions are marked as voided, not permanently deleted.

### Test Steps:

1. **Create a Test Transaction:**
   - Go to Sales
   - Add any product to cart
   - Complete the transaction
   - Note the Transaction ID (e.g., TXN042)

2. **Go to Reports:**
   - Click "Reports" in sidebar
   - Find your transaction in the "All Transactions" table

3. **Check Button Appearance:**
   - Look at the Actions column
   - You should see 3 buttons:
     - 👁 (View)
     - ✏ (Edit)
     - **⊘ Void** (Orange button - not red delete)

4. **Test Void Functionality:**
   - Click the **"⊘ Void"** button
   - Dialog appears asking for reason
   - Enter reason: `Test void function`
   - Click OK
   - Confirm the void action

5. **Verify Void Happened:**
   - Transaction should remain in the table
   - It should be marked as "VOIDED" or have a visual indicator
   - Inventory should be restored

6. **Try to Void Again:**
   - Click the Void button on the same transaction
   - Should show warning: "Transaction is already voided"

### ✅ Expected Result:
- No "Delete" button (replaced with "Void")
- Voided transactions stay in records
- Inventory is restored when voiding
- Can see void reason in transaction details
- Cannot void a transaction twice

### ❌ Failure Indicators:
- Still see a red "Delete" button
- Transaction disappears after voiding
- Can void the same transaction multiple times

---

## 🔍 Additional Tests

### Test 1: Verify Batch Info Stored in Database

**Purpose:** Confirm batch tracking is working

1. Complete a transaction with any product
2. Use a database viewer to check the `sale_items` table
3. Look at the `batch_info` column for that transaction

**Expected:** Should see JSON like:
```json
[{"batchId":1,"batchNumber":"BATCH-MED001-20251207-001","quantity":50,"expiryDate":"2026-01-15"},{"batchId":2,"batchNumber":"BATCH-MED001-20251207-002","quantity":10,"expiryDate":"2026-02-20"}]
```

---

### Test 2: Verify Void Status in Database

**Purpose:** Confirm void tracking is working

1. Void a transaction
2. Check the `sales` table in database
3. Find that transaction

**Expected:**
- `voided` column = `true` or `1`
- `void_date` = timestamp of when voided
- `void_reason` = your entered reason

---

## 📊 Test Summary Checklist

Use this to track your testing:

- [ ] Bug #1: No duplicate products when adding stock
- [ ] Bug #2: Expiry shows "N/A" when stock is 0
- [ ] Bug #3: Perfect batch restoration (200 + 1 = 200 + 1)
- [ ] Bug #4: Cart persists across module navigation
- [ ] Bug #5: Void button exists (no delete button)
- [ ] Bug #5: Voided transactions stay in records
- [ ] Bug #5: Cannot void same transaction twice
- [ ] Additional: Batch info stored in database
- [ ] Additional: Void status stored in database

---

## 🐛 What to Do If Tests Fail

1. **Check Console Logs:**
   - Look at your terminal/console output
   - Search for error messages or stack traces

2. **Verify Database Schema:**
   - Make sure you ran the ALTER TABLE statements
   - Check that new columns exist

3. **Restart Application:**
   - Stop and restart `DrugstoreApplication`
   - Clear any caches

4. **Check Browser Console (if applicable):**
   - Press F12 in browser
   - Look for JavaScript errors

5. **Report Issues:**
   - Note which test failed
   - Copy any error messages
   - Take screenshots of unexpected behavior

---

## 🎯 Success Criteria

**All fixes are working correctly if:**
- ✅ All 9 checkboxes above are checked
- ✅ No errors in console logs
- ✅ Batch restoration is PERFECT (exact amounts to exact batches)
- ✅ Cart survives navigation
- ✅ Void system replaces delete system

Good luck with testing! 🚀
