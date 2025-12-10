-- =========================================
-- Database Migration Script
-- Bug Fixes: Batch Tracking & Void System
-- Date: December 7, 2025
-- =========================================

-- Add batch tracking to sale_items table
ALTER TABLE sale_items 
ADD COLUMN IF NOT EXISTS batch_info TEXT 
COMMENT 'JSON string storing batch deductions for perfect restoration';

-- Add void tracking to sales table
ALTER TABLE sales 
ADD COLUMN IF NOT EXISTS voided BOOLEAN DEFAULT FALSE 
COMMENT 'Whether transaction is voided';

ALTER TABLE sales 
ADD COLUMN IF NOT EXISTS void_date TIMESTAMP NULL 
COMMENT 'When transaction was voided';

ALTER TABLE sales 
ADD COLUMN IF NOT EXISTS void_reason VARCHAR(500) NULL 
COMMENT 'Reason for voiding transaction';

-- Create index for faster void lookups
CREATE INDEX IF NOT EXISTS idx_sales_voided ON sales(voided);

-- Verify the changes
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'sale_items' AND COLUMN_NAME = 'batch_info'
UNION ALL
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'sales' AND COLUMN_NAME IN ('voided', 'void_date', 'void_reason');

-- Success message
SELECT 'Database migration completed successfully!' AS status;
