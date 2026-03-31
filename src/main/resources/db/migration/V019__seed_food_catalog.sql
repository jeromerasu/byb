-- V019: Seed food_catalog with common foods across major categories
-- All entries are system entries (is_system=true, created_by_user_id=NULL)

INSERT INTO food_catalog (name, category, serving_size, calories, protein_grams, carbs_grams, fat_grams, fiber_grams, is_system, created_by_user_id)
VALUES

-- PROTEIN
('Chicken Breast',       'PROTEIN', '100g',       165,  31.0,  0.0,  3.6,  0.0,  TRUE, NULL),
('Salmon Fillet',        'PROTEIN', '100g',       208,  20.0,  0.0,  13.0, 0.0,  TRUE, NULL),
('Ground Turkey',        'PROTEIN', '100g',       170,  21.0,  0.0,  9.0,  0.0,  TRUE, NULL),
('Eggs',                 'PROTEIN', '2 large',    143,  13.0,  1.0,  10.0, 0.0,  TRUE, NULL),
('Greek Yogurt',         'PROTEIN', '170g',       100,  17.0,  6.0,  0.7,  0.0,  TRUE, NULL),
('Whey Protein Shake',   'PROTEIN', '1 scoop',    120,  24.0,  3.0,  1.5,  0.0,  TRUE, NULL),
('Tuna',                 'PROTEIN', '100g',       130,  29.0,  0.0,  1.0,  0.0,  TRUE, NULL),
('Steak Sirloin',        'PROTEIN', '100g',       206,  26.0,  0.0,  11.0, 0.0,  TRUE, NULL),
('Tofu',                 'PROTEIN', '100g',       76,   8.0,   2.0,  4.8,  0.3,  TRUE, NULL),
('Shrimp',               'PROTEIN', '100g',       85,   20.0,  0.0,  0.5,  0.0,  TRUE, NULL),

-- CARB
('Brown Rice',           'CARB',    '1 cup cooked', 216, 5.0,  45.0, 1.8,  3.5,  TRUE, NULL),
('Sweet Potato',         'CARB',    '1 medium',   103,  2.0,   24.0, 0.1,  3.8,  TRUE, NULL),
('Oatmeal',              'CARB',    '1 cup cooked', 154, 5.0,  27.0, 2.6,  4.0,  TRUE, NULL),
('Quinoa',               'CARB',    '1 cup cooked', 222, 8.0,  39.0, 3.5,  5.0,  TRUE, NULL),
('Whole Wheat Bread',    'CARB',    '2 slices',   160,  8.0,   28.0, 2.0,  4.0,  TRUE, NULL),
('White Rice',           'CARB',    '1 cup cooked', 206, 4.0,  45.0, 0.4,  0.6,  TRUE, NULL),
('Pasta',                'CARB',    '1 cup cooked', 220, 8.0,  43.0, 1.3,  2.5,  TRUE, NULL),
('Banana',               'CARB',    '1 medium',   105,  1.3,   27.0, 0.4,  3.1,  TRUE, NULL),

-- FAT
('Avocado',              'FAT',     '1/2 medium', 160,  2.0,   9.0,  15.0, 6.7,  TRUE, NULL),
('Almonds',              'FAT',     '28g',        164,  6.0,   6.0,  14.0, 3.5,  TRUE, NULL),
('Peanut Butter',        'FAT',     '2 tbsp',     190,  7.0,   7.0,  16.0, 1.5,  TRUE, NULL),
('Olive Oil',            'FAT',     '1 tbsp',     119,  0.0,   0.0,  13.5, 0.0,  TRUE, NULL),
('Walnuts',              'FAT',     '28g',        185,  4.0,   4.0,  18.0, 1.9,  TRUE, NULL),
('Coconut Oil',          'FAT',     '1 tbsp',     121,  0.0,   0.0,  13.5, 0.0,  TRUE, NULL),

-- VEGETABLE
('Broccoli',             'VEGETABLE', '1 cup',    55,   4.0,   11.0, 0.6,  5.1,  TRUE, NULL),
('Spinach',              'VEGETABLE', '1 cup raw', 7,   1.0,   1.0,  0.1,  0.7,  TRUE, NULL),
('Asparagus',            'VEGETABLE', '1 cup',    27,   3.0,   5.0,  0.2,  2.8,  TRUE, NULL),
('Bell Pepper',          'VEGETABLE', '1 medium', 30,   1.0,   7.0,  0.3,  2.5,  TRUE, NULL),
('Green Beans',          'VEGETABLE', '1 cup',    31,   2.0,   7.0,  0.1,  4.0,  TRUE, NULL),
('Kale',                 'VEGETABLE', '1 cup raw', 33,  3.0,   6.0,  0.6,  1.3,  TRUE, NULL),

-- FRUIT
('Apple',                'FRUIT',   '1 medium',   95,   0.5,   25.0, 0.3,  4.4,  TRUE, NULL),
('Blueberries',          'FRUIT',   '1 cup',      85,   1.0,   21.0, 0.5,  3.6,  TRUE, NULL),
('Strawberries',         'FRUIT',   '1 cup',      49,   1.0,   12.0, 0.5,  3.0,  TRUE, NULL),
('Orange',               'FRUIT',   '1 medium',   62,   1.2,   15.0, 0.2,  3.1,  TRUE, NULL),

-- DAIRY
('Cottage Cheese',       'DAIRY',   '1 cup',      206,  28.0,  6.0,  9.0,  0.0,  TRUE, NULL),
('Whole Milk',           'DAIRY',   '1 cup',      149,  8.0,   12.0, 8.0,  0.0,  TRUE, NULL),
('Cheddar Cheese',       'DAIRY',   '28g',        113,  7.0,   0.4,  9.0,  0.0,  TRUE, NULL),

-- SNACK
('Protein Bar',          'SNACK',   '1 bar',      210,  20.0,  25.0, 7.0,  3.0,  TRUE, NULL),
('Rice Cakes',           'SNACK',   '2 cakes',    70,   1.4,   15.0, 0.4,  0.4,  TRUE, NULL),
('Trail Mix',            'SNACK',   '28g',        140,  4.0,   13.0, 9.0,  1.5,  TRUE, NULL),
('Dark Chocolate',       'SNACK',   '28g',        155,  2.0,   17.0, 9.0,  2.0,  TRUE, NULL);
