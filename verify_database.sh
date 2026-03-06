#!/bin/bash
# Quick verification script for expense database

echo "=== Expense Database Verification ==="
echo ""

echo "1. Checking Expense Categories..."
docker exec postgres psql -U postgres -d expense_db -c "SELECT COUNT(*) as total_categories FROM expense_categories;"

echo ""
echo "2. Checking Expenses..."
docker exec postgres psql -U postgres -d expense_db -c "SELECT COUNT(*) as total_expenses FROM expenses;"

echo ""
echo "3. Expenses by User..."
docker exec postgres psql -U postgres -d expense_db -c "SELECT username, COUNT(*) as count, SUM(amount) as total FROM expenses GROUP BY username ORDER BY total DESC;"

echo ""
echo "4. Sample Expenses (Last 5)..."
docker exec postgres psql -U postgres -d expense_db -c "SELECT username, amount, date, description, payment_method FROM expenses ORDER BY date DESC LIMIT 5;"

echo ""
echo "5. Categories with Subcategories..."
docker exec postgres psql -U postgres -d expense_db -c "SELECT CASE WHEN parent_id IS NULL THEN '└─ ' || name ELSE '   ├─ ' || name END as category FROM expense_categories ORDER BY parent_id, name;"

echo ""
echo "=== Verification Complete ==="

