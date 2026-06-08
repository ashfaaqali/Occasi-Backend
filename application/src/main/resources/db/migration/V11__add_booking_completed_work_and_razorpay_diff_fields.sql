-- Migration to add completed_work_image_url and razorpay diff order/payment columns to booking table
ALTER TABLE booking ADD COLUMN IF NOT EXISTS completed_work_image_url VARCHAR(500);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS razorpay_diff_order_id VARCHAR(255);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS razorpay_diff_payment_id VARCHAR(255);
