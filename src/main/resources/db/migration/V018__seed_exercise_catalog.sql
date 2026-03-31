-- V018: Seed exercise_catalog with common gym exercises
-- All entries are system entries (is_system=true, created_by_user_id=NULL)

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
VALUES

-- STRENGTH: Chest
('Barbell Bench Press',      'STRENGTH', '{CHEST,TRICEPS,SHOULDERS}', '{BARBELL,BENCH}',         'INTERMEDIATE', NULL, NULL, 'Lie flat on a bench, grip the bar slightly wider than shoulder width, lower to chest and press up.', TRUE, NULL),
('Incline Dumbbell Press',   'STRENGTH', '{CHEST,SHOULDERS}',         '{DUMBBELL,BENCH}',        'INTERMEDIATE', NULL, NULL, 'Set bench to 30-45 degrees incline, press dumbbells from shoulder level to full extension above chest.', TRUE, NULL),
('Dumbbell Fly',             'STRENGTH', '{CHEST}',                   '{DUMBBELL,BENCH}',        'BEGINNER',     NULL, NULL, 'Lie flat on a bench, hold dumbbells above chest with slight elbow bend, lower arms out wide then bring back together.', TRUE, NULL),
('Cable Fly',                'STRENGTH', '{CHEST}',                   '{CABLE}',                 'BEGINNER',     NULL, NULL, 'Stand between cable machine pulleys set at shoulder height, pull handles together in front of chest in a wide arc.', TRUE, NULL),
('Push Up',                  'STRENGTH', '{CHEST,TRICEPS,SHOULDERS}', '{BODYWEIGHT}',            'BEGINNER',     NULL, NULL, 'Start in plank position with hands shoulder-width apart, lower chest to ground then push back up.', TRUE, NULL),
('Dip',                      'STRENGTH', '{CHEST,TRICEPS}',           '{BODYWEIGHT,DIP_BAR}',   'INTERMEDIATE', NULL, NULL, 'Grip parallel bars, lower body by bending elbows until upper arms are parallel to floor, then press back up.', TRUE, NULL),

-- STRENGTH: Legs
('Barbell Back Squat',       'STRENGTH', '{LEGS,GLUTES,CORE}',         '{BARBELL,SQUAT_RACK}',   'INTERMEDIATE', NULL, NULL, 'Bar rests on upper traps, feet shoulder-width apart, squat down until thighs are parallel to floor, drive back up.', TRUE, NULL),
('Leg Press',                'STRENGTH', '{LEGS,GLUTES}',              '{MACHINE}',              'BEGINNER',     NULL, NULL, 'Sit in leg press machine, place feet shoulder-width on platform, lower weight by bending knees then press back.', TRUE, NULL),
('Romanian Deadlift',        'STRENGTH', '{HAMSTRINGS,GLUTES,BACK}',   '{BARBELL}',              'INTERMEDIATE', NULL, NULL, 'Hold barbell at hip level, hinge at hips pushing them back while lowering bar along legs, return by driving hips forward.', TRUE, NULL),
('Leg Curl',                 'STRENGTH', '{HAMSTRINGS}',               '{MACHINE}',              'BEGINNER',     NULL, NULL, 'Lie face down on leg curl machine, curl heels toward glutes against resistance, then lower slowly.', TRUE, NULL),
('Leg Extension',            'STRENGTH', '{QUADRICEPS}',               '{MACHINE}',              'BEGINNER',     NULL, NULL, 'Sit in leg extension machine, extend legs to full lockout against resistance, then lower slowly.', TRUE, NULL),
('Calf Raise',               'STRENGTH', '{CALVES}',                   '{MACHINE,BODYWEIGHT}',   'BEGINNER',     NULL, NULL, 'Stand with balls of feet on an elevated surface, raise heels as high as possible, then lower below platform level.', TRUE, NULL),
('Bulgarian Split Squat',    'STRENGTH', '{LEGS,GLUTES}',              '{DUMBBELL,BENCH}',       'INTERMEDIATE', NULL, NULL, 'Rear foot elevated on bench, front foot forward, lower rear knee toward ground then drive back up through front heel.', TRUE, NULL),
('Dumbbell Lunge',           'STRENGTH', '{LEGS,GLUTES}',              '{DUMBBELL}',             'BEGINNER',     NULL, NULL, 'Hold dumbbells at sides, step forward with one leg and lower back knee toward floor, return to standing and alternate.', TRUE, NULL),
('Hip Thrust',               'STRENGTH', '{GLUTES,HAMSTRINGS}',        '{BARBELL,BENCH}',        'INTERMEDIATE', NULL, NULL, 'Shoulders on bench, barbell across hips, drive hips up by squeezing glutes until body is straight, lower and repeat.', TRUE, NULL),

-- STRENGTH: Back
('Conventional Deadlift',    'STRENGTH', '{BACK,LEGS,CORE}',           '{BARBELL}',              'ADVANCED',     NULL, NULL, 'Grip bar shoulder-width, flat back, push floor away to stand with the bar, then hinge back down under control.', TRUE, NULL),
('Pull Up',                  'STRENGTH', '{BACK,BICEPS}',              '{BODYWEIGHT,PULL_UP_BAR}', 'INTERMEDIATE', NULL, NULL, 'Hang from bar with overhand grip, pull chest to bar by driving elbows toward hips, lower with control.', TRUE, NULL),
('Lat Pulldown',             'STRENGTH', '{BACK,BICEPS}',              '{CABLE,MACHINE}',        'BEGINNER',     NULL, NULL, 'Grip wide bar overhead at cable machine, pull bar to upper chest while leaning slightly back, return slowly.', TRUE, NULL),
('Barbell Row',              'STRENGTH', '{BACK,BICEPS}',              '{BARBELL}',              'INTERMEDIATE', NULL, NULL, 'Hip-hinge to 45 degrees, grip barbell, row to lower chest while keeping elbows close to torso, lower with control.', TRUE, NULL),
('Seated Cable Row',         'STRENGTH', '{BACK,BICEPS}',              '{CABLE}',                'BEGINNER',     NULL, NULL, 'Sit at cable row machine, pull handle to abdomen while driving elbows back, squeeze shoulder blades, return slowly.', TRUE, NULL),
('Face Pull',                'STRENGTH', '{SHOULDERS,BACK}',           '{CABLE}',                'BEGINNER',     NULL, NULL, 'Set cable at face height with rope attachment, pull toward face with elbows flared high, externally rotate at end.', TRUE, NULL),

-- STRENGTH: Shoulders
('Barbell Overhead Press',   'STRENGTH', '{SHOULDERS,TRICEPS}',        '{BARBELL,SQUAT_RACK}',   'INTERMEDIATE', NULL, NULL, 'Bar at collar bone height, press overhead to full lockout, lower back to starting position under control.', TRUE, NULL),
('Dumbbell Shoulder Press',  'STRENGTH', '{SHOULDERS,TRICEPS}',        '{DUMBBELL}',             'BEGINNER',     NULL, NULL, 'Sit or stand, hold dumbbells at shoulder height, press overhead to full extension then lower back down.', TRUE, NULL),
('Lateral Raise',            'STRENGTH', '{SHOULDERS}',                '{DUMBBELL}',             'BEGINNER',     NULL, NULL, 'Stand with dumbbells at sides, raise arms out to shoulder height with slight elbow bend, lower slowly.', TRUE, NULL),

-- STRENGTH: Arms
('Barbell Curl',             'STRENGTH', '{BICEPS}',                   '{BARBELL}',              'BEGINNER',     NULL, NULL, 'Stand with barbell at thigh level, curl to shoulder height by bending elbows, lower under control.', TRUE, NULL),
('Dumbbell Hammer Curl',     'STRENGTH', '{BICEPS,FOREARMS}',          '{DUMBBELL}',             'BEGINNER',     NULL, NULL, 'Hold dumbbells with neutral grip (palms facing each other), curl to shoulder height, lower under control.', TRUE, NULL),
('Tricep Pushdown',          'STRENGTH', '{TRICEPS}',                  '{CABLE}',                'BEGINNER',     NULL, NULL, 'Stand at cable machine with bar at upper pulley, push bar down to full extension keeping elbows at sides, return slowly.', TRUE, NULL),
('Overhead Tricep Extension','STRENGTH', '{TRICEPS}',                  '{DUMBBELL,CABLE}',       'BEGINNER',     NULL, NULL, 'Hold weight overhead with both hands, lower behind head by bending elbows, extend back to start.', TRUE, NULL),

-- STRENGTH: Core
('Plank',                    'STRENGTH', '{CORE}',                     '{BODYWEIGHT}',           'BEGINNER',     NULL, NULL, 'Hold a push-up position on forearms and toes, keep body straight from head to heels, breathe steadily.', TRUE, NULL),
('Russian Twist',            'STRENGTH', '{CORE}',                     '{BODYWEIGHT,DUMBBELL}',  'BEGINNER',     NULL, NULL, 'Sit with knees bent and feet off floor, lean back slightly, rotate torso side to side touching floor or holding weight.', TRUE, NULL),

-- CARDIO
('Treadmill Run',            'CARDIO',   '{CARDIO}',                   '{MACHINE}',              'BEGINNER',     NULL, NULL, 'Set treadmill to desired speed and incline, maintain upright posture and consistent cadence throughout session.', TRUE, NULL),
('Stationary Bike',          'CARDIO',   '{CARDIO,LEGS}',              '{MACHINE}',              'BEGINNER',     NULL, NULL, 'Adjust seat height so knee has slight bend at bottom of pedal stroke, maintain steady pace at target heart rate.', TRUE, NULL),
('Rowing Machine',           'CARDIO',   '{CARDIO,BACK,ARMS}',         '{MACHINE}',              'INTERMEDIATE', NULL, NULL, 'Drive with legs first, then lean back and pull handle to lower chest, return by reversing the sequence.', TRUE, NULL),
('Jump Rope',                'CARDIO',   '{CARDIO,CALVES}',            '{JUMP_ROPE}',            'BEGINNER',     NULL, NULL, 'Keep elbows close to body, rotate wrists to swing rope, jump lightly on balls of feet with each pass.', TRUE, NULL),
('Stair Climber',            'CARDIO',   '{CARDIO,LEGS,GLUTES}',       '{MACHINE}',              'INTERMEDIATE', NULL, NULL, 'Maintain upright posture with slight forward lean, step fully onto each stair, avoid holding handrails for full benefit.', TRUE, NULL),

-- FLEXIBILITY
('Yoga Flow',                'FLEXIBILITY', '{FLEXIBILITY,CORE}',      '{BODYWEIGHT}',           'BEGINNER',     NULL, NULL, 'Move through a series of linked yoga poses coordinating breath with movement to improve flexibility and mindfulness.', TRUE, NULL),
('Static Stretching',        'FLEXIBILITY', '{FLEXIBILITY}',           '{BODYWEIGHT}',           'BEGINNER',     NULL, NULL, 'Hold each stretch position for 20-30 seconds without bouncing, focusing on major muscle groups after exercise.', TRUE, NULL);
