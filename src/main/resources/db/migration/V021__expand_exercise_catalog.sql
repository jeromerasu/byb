-- V021: Expand exercise_catalog to ~150 total exercises
-- Part 1: Update existing 37 exercises with ExerciseDB GIF URLs
-- Part 2: Insert ~113 new exercises across bodyweight, dumbbell, barbell, cable, machine, bands, cardio

-- ============================================================
-- PART 1: UPDATE existing exercises with video/thumbnail URLs
-- URL format: https://static.exercisedb.dev/media/{ID}.gif
-- ============================================================

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/EIeI8Vf.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/EIeI8Vf.gif',
    updated_at    = NOW()
WHERE name = 'Barbell Bench Press' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/JHBRZa9.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/JHBRZa9.gif',
    updated_at    = NOW()
WHERE name = 'Incline Dumbbell Press' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/q0bTaXl.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/q0bTaXl.gif',
    updated_at    = NOW()
WHERE name = 'Dumbbell Fly' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/MYkFWFm.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/MYkFWFm.gif',
    updated_at    = NOW()
WHERE name = 'Cable Fly' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/Wk7BRHE.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/Wk7BRHE.gif',
    updated_at    = NOW()
WHERE name = 'Push Up' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/kh4mNBP.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/kh4mNBP.gif',
    updated_at    = NOW()
WHERE name = 'Dip' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/XGhBMn0.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/XGhBMn0.gif',
    updated_at    = NOW()
WHERE name = 'Barbell Back Squat' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/GNeodj1.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/GNeodj1.gif',
    updated_at    = NOW()
WHERE name = 'Leg Press' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/qa18JXi.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/qa18JXi.gif',
    updated_at    = NOW()
WHERE name = 'Romanian Deadlift' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/feKk6ND.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/feKk6ND.gif',
    updated_at    = NOW()
WHERE name = 'Leg Curl' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/y1pFQ3Y.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/y1pFQ3Y.gif',
    updated_at    = NOW()
WHERE name = 'Leg Extension' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/2dkaBre.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/2dkaBre.gif',
    updated_at    = NOW()
WHERE name = 'Calf Raise' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/eDMTsnp.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/eDMTsnp.gif',
    updated_at    = NOW()
WHERE name = 'Bulgarian Split Squat' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/WGE82wh.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/WGE82wh.gif',
    updated_at    = NOW()
WHERE name = 'Dumbbell Lunge' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/NEbIO3s.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/NEbIO3s.gif',
    updated_at    = NOW()
WHERE name = 'Hip Thrust' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/xKvip1W.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/xKvip1W.gif',
    updated_at    = NOW()
WHERE name = 'Conventional Deadlift' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/hbIxky5.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/hbIxky5.gif',
    updated_at    = NOW()
WHERE name = 'Pull Up' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/vJ5A2BN.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/vJ5A2BN.gif',
    updated_at    = NOW()
WHERE name = 'Lat Pulldown' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/Jg0K4fw.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/Jg0K4fw.gif',
    updated_at    = NOW()
WHERE name = 'Barbell Row' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/rpRWrO4.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/rpRWrO4.gif',
    updated_at    = NOW()
WHERE name = 'Seated Cable Row' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/KKqAVfD.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/KKqAVfD.gif',
    updated_at    = NOW()
WHERE name = 'Face Pull' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/cGVPgvN.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/cGVPgvN.gif',
    updated_at    = NOW()
WHERE name = 'Barbell Overhead Press' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/RJHAQJD.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/RJHAQJD.gif',
    updated_at    = NOW()
WHERE name = 'Dumbbell Shoulder Press' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/Wfwcmcr.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/Wfwcmcr.gif',
    updated_at    = NOW()
WHERE name = 'Lateral Raise' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/JBvfSNc.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/JBvfSNc.gif',
    updated_at    = NOW()
WHERE name = 'Barbell Curl' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/X14FNBK.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/X14FNBK.gif',
    updated_at    = NOW()
WHERE name = 'Dumbbell Hammer Curl' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/IOz5ioq.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/IOz5ioq.gif',
    updated_at    = NOW()
WHERE name = 'Tricep Pushdown' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/MnMDBek.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/MnMDBek.gif',
    updated_at    = NOW()
WHERE name = 'Overhead Tricep Extension' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/ooAuqIB.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/ooAuqIB.gif',
    updated_at    = NOW()
WHERE name = 'Plank' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/XVDdcoj.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/XVDdcoj.gif',
    updated_at    = NOW()
WHERE name = 'Russian Twist' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/KCPHUoD.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/KCPHUoD.gif',
    updated_at    = NOW()
WHERE name = 'Treadmill Run' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/DO3MuGk.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/DO3MuGk.gif',
    updated_at    = NOW()
WHERE name = 'Stationary Bike' AND is_system = TRUE;

UPDATE exercise_catalog SET
    video_url     = 'https://static.exercisedb.dev/media/hiOcEEk.gif',
    thumbnail_url = 'https://static.exercisedb.dev/media/hiOcEEk.gif',
    updated_at    = NOW()
WHERE name = 'Jump Rope' AND is_system = TRUE;

-- ============================================================
-- PART 2: INSERT ~113 new exercises (idempotent via NOT EXISTS)
-- ============================================================

-- ----------------------------------------
-- BODYWEIGHT (~28)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Mountain Climber', 'STRENGTH', '{CORE,SHOULDERS,LEGS}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Start in a high plank, alternate driving knees toward chest in a running motion while keeping hips level. Move as fast as possible while maintaining form.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Mountain Climber' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Burpee', 'CARDIO', '{CARDIO,CHEST,LEGS,CORE}', '{BODYWEIGHT}', 'INTERMEDIATE', NULL, NULL, 'From standing, squat down and place hands on floor, jump feet back to plank, perform a push-up, jump feet back to hands, then explosively jump up with arms overhead.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Burpee' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Jumping Jack', 'CARDIO', '{CARDIO,SHOULDERS,LEGS}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Start standing with feet together and arms at sides, jump feet wide while raising arms overhead, then return to start in one fluid motion.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Jumping Jack' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Box Jump', 'STRENGTH', '{LEGS,GLUTES,CALVES}', '{BODYWEIGHT,PLYO_BOX}', 'INTERMEDIATE', NULL, NULL, 'Stand facing a sturdy box, bend knees and swing arms to load the jump, explode upward landing softly on both feet on the box, then step down.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Box Jump' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Bear Crawl', 'STRENGTH', '{CORE,SHOULDERS,LEGS}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Start on all fours with knees hovering one inch off the ground, move opposite hand and foot forward simultaneously while keeping hips low and core braced.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Bear Crawl' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Pike Push Up', 'STRENGTH', '{SHOULDERS,TRICEPS}', '{BODYWEIGHT}', 'INTERMEDIATE', NULL, NULL, 'Form an inverted V with hips high and hands and feet on the floor, bend elbows to lower your head toward the ground, then push back up.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Pike Push Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Diamond Push Up', 'STRENGTH', '{TRICEPS,CHEST}', '{BODYWEIGHT}', 'INTERMEDIATE', NULL, NULL, 'Form a diamond shape with index fingers and thumbs on the ground, perform a push-up keeping elbows close to the body throughout the movement.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Diamond Push Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Wide Push Up', 'STRENGTH', '{CHEST,SHOULDERS}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Place hands wider than shoulder-width apart, lower chest toward the floor while keeping body straight, press back up to starting position.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Wide Push Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Decline Push Up', 'STRENGTH', '{CHEST,SHOULDERS,TRICEPS}', '{BODYWEIGHT,BENCH}', 'INTERMEDIATE', NULL, NULL, 'Elevate feet on a bench or box with hands on the floor, perform a push-up targeting the upper chest, keeping core tight throughout.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Decline Push Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Archer Push Up', 'STRENGTH', '{CHEST,TRICEPS,SHOULDERS}', '{BODYWEIGHT}', 'ADVANCED', NULL, NULL, 'Start in wide push-up position, lower to one side while extending the opposite arm straight, alternate sides each rep to build unilateral strength.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Archer Push Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Pistol Squat', 'STRENGTH', '{LEGS,GLUTES,CORE}', '{BODYWEIGHT}', 'ADVANCED', NULL, NULL, 'Stand on one leg with the other extended forward, lower into a full squat on the standing leg while keeping the extended leg off the ground, then press back up.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Pistol Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Jump Squat', 'STRENGTH', '{LEGS,GLUTES,CALVES}', '{BODYWEIGHT}', 'INTERMEDIATE', NULL, NULL, 'Perform a standard squat, then explosively jump off the ground at the top, land softly with bent knees and immediately descend into the next rep.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Jump Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Wall Sit', 'STRENGTH', '{QUADRICEPS,GLUTES}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Slide back down a wall until thighs are parallel to the floor and knees are at 90 degrees, hold position with back flat against the wall.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Wall Sit' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Glute Bridge', 'STRENGTH', '{GLUTES,HAMSTRINGS,CORE}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Lie on back with knees bent and feet flat, drive hips upward by squeezing glutes until body forms a straight line from shoulders to knees, lower and repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Glute Bridge' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Single Leg Deadlift', 'STRENGTH', '{HAMSTRINGS,GLUTES,BACK}', '{BODYWEIGHT}', 'INTERMEDIATE', NULL, NULL, 'Stand on one leg, hinge forward at the hip while extending the free leg behind you, keep back flat until feeling a hamstring stretch, then return upright.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Single Leg Deadlift' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Inverted Row', 'STRENGTH', '{BACK,BICEPS}', '{BODYWEIGHT,BARBELL}', 'BEGINNER', NULL, NULL, 'Hang beneath a barbell or rings with arms extended and body straight, pull chest up to the bar by driving elbows back, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Inverted Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Chin Up', 'STRENGTH', '{BACK,BICEPS}', '{BODYWEIGHT,PULL_UP_BAR}', 'INTERMEDIATE', NULL, NULL, 'Grip a bar with palms facing you at shoulder width, pull chin above the bar by driving elbows down and squeezing biceps, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Chin Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Hanging Leg Raise', 'STRENGTH', '{CORE,HIP_FLEXORS}', '{BODYWEIGHT,PULL_UP_BAR}', 'INTERMEDIATE', NULL, NULL, 'Hang from a pull-up bar with arms extended, brace core and raise straight legs to hip height or above, lower slowly without swinging.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Hanging Leg Raise' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'L-Sit', 'STRENGTH', '{CORE,HIP_FLEXORS,TRICEPS}', '{BODYWEIGHT}', 'ADVANCED', NULL, NULL, 'Support your body on two parallel bars or on the floor, extend legs straight out in front forming an L shape, hold while keeping hips from dropping.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'L-Sit' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Flutter Kick', 'STRENGTH', '{CORE,HIP_FLEXORS}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Lie on your back with legs extended and slightly raised, alternate kicking legs up and down in a small controlled motion while keeping the lower back pressed down.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Flutter Kick' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Bicycle Crunch', 'STRENGTH', '{CORE}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Lie on back with hands behind head, bring opposite elbow and knee together while extending the other leg, alternate sides in a pedaling motion.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Bicycle Crunch' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dead Bug', 'STRENGTH', '{CORE}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Lie on back with arms extended to the ceiling and knees bent at 90 degrees, slowly lower opposite arm and leg toward the floor while keeping lower back pressed down, return and alternate.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dead Bug' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Bird Dog', 'STRENGTH', '{CORE,BACK,GLUTES}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Start on all fours with a neutral spine, extend the opposite arm and leg simultaneously while keeping hips level, hold briefly then alternate sides.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Bird Dog' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Superman', 'STRENGTH', '{BACK,GLUTES}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Lie face down with arms extended overhead, simultaneously lift arms, chest, and legs off the floor by squeezing glutes and back muscles, hold briefly then lower.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Superman' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Crab Walk', 'STRENGTH', '{CORE,TRICEPS,GLUTES}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Sit on the floor with hands behind and feet flat, lift hips off the ground, then walk forward or backward by moving opposite hand and foot simultaneously.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Crab Walk' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'High Knees', 'CARDIO', '{CARDIO,LEGS,CORE}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Run in place driving knees up to hip height alternately, pump arms in opposition, maintain a fast cadence to elevate heart rate.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'High Knees' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Butt Kicks', 'CARDIO', '{CARDIO,HAMSTRINGS}', '{BODYWEIGHT}', 'BEGINNER', NULL, NULL, 'Run in place kicking heels up toward your glutes with each stride, keep torso upright and arms pumping, maintain a quick rhythm.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Butt Kicks' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Tuck Jump', 'STRENGTH', '{LEGS,GLUTES,CORE}', '{BODYWEIGHT}', 'INTERMEDIATE', NULL, NULL, 'Stand with feet shoulder-width apart, bend knees slightly and jump explosively, pulling both knees to chest at the peak, land softly and immediately repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Tuck Jump' AND is_system = TRUE);

-- ----------------------------------------
-- DUMBBELL (~20)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Bench Press', 'STRENGTH', '{CHEST,TRICEPS,SHOULDERS}', '{DUMBBELL,BENCH}', 'BEGINNER', NULL, NULL, 'Lie flat on a bench holding dumbbells at chest level, press both up to full extension above the chest, lower slowly to starting position.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Bench Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Row', 'STRENGTH', '{BACK,BICEPS}', '{DUMBBELL,BENCH}', 'BEGINNER', NULL, NULL, 'Place one hand and knee on a bench for support, hold a dumbbell in the opposite hand, row it to your hip by driving the elbow back, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Romanian Deadlift', 'STRENGTH', '{HAMSTRINGS,GLUTES,BACK}', '{DUMBBELL}', 'BEGINNER', NULL, NULL, 'Hold dumbbells in front of thighs, hinge at hips pushing them back while lowering dumbbells along the legs, keep back flat and return by driving hips forward.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Romanian Deadlift' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Goblet Squat', 'STRENGTH', '{LEGS,GLUTES,CORE}', '{DUMBBELL}', 'BEGINNER', NULL, NULL, 'Hold a dumbbell vertically at chest level, squat down keeping elbows inside knees at the bottom, drive through heels to return to standing.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Goblet Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Step Up', 'STRENGTH', '{LEGS,GLUTES}', '{DUMBBELL,BENCH}', 'BEGINNER', NULL, NULL, 'Hold dumbbells at sides, step one foot onto a bench, drive through that heel to lift the body up, step back down and alternate legs.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Step Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Pullover', 'STRENGTH', '{CHEST,BACK,TRICEPS}', '{DUMBBELL,BENCH}', 'INTERMEDIATE', NULL, NULL, 'Lie on a bench holding one dumbbell with both hands over your chest, lower the weight in an arc behind your head until you feel a chest stretch, pull back to start.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Pullover' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Reverse Fly', 'STRENGTH', '{BACK,SHOULDERS}', '{DUMBBELL}', 'BEGINNER', NULL, NULL, 'Hinge forward at the hips holding dumbbells, raise arms out to the sides with a slight elbow bend until level with your back, squeeze shoulder blades and lower.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Reverse Fly' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Front Raise', 'STRENGTH', '{SHOULDERS}', '{DUMBBELL}', 'BEGINNER', NULL, NULL, 'Stand holding dumbbells at thigh level, raise both arms straight out in front to shoulder height, lower slowly and repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Front Raise' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Shrug', 'STRENGTH', '{TRAPS}', '{DUMBBELL}', 'BEGINNER', NULL, NULL, 'Hold dumbbells at sides with arms straight, elevate shoulders straight up toward ears as high as possible, hold briefly then lower slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Shrug' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Concentration Curl', 'STRENGTH', '{BICEPS}', '{DUMBBELL}', 'BEGINNER', NULL, NULL, 'Sit on a bench, rest the back of one upper arm on the inner thigh, curl the dumbbell to shoulder height focusing on the bicep peak, lower fully.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Concentration Curl' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Skull Crusher', 'STRENGTH', '{TRICEPS}', '{DUMBBELL,BENCH}', 'INTERMEDIATE', NULL, NULL, 'Lie on a bench holding dumbbells above chest, lower them toward your forehead by bending only the elbows, extend back to the start keeping upper arms stationary.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Skull Crusher' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Kickback', 'STRENGTH', '{TRICEPS}', '{DUMBBELL}', 'BEGINNER', NULL, NULL, 'Hinge forward with upper arm parallel to the floor, extend the forearm back to full lockout by straightening the elbow, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Kickback' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Calf Raise', 'STRENGTH', '{CALVES}', '{DUMBBELL}', 'BEGINNER', NULL, NULL, 'Stand holding dumbbells at your sides with feet hip-width apart, raise onto the balls of your feet as high as possible, lower heels below the starting point for a full stretch.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Calf Raise' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Arnold Press', 'STRENGTH', '{SHOULDERS,TRICEPS}', '{DUMBBELL}', 'INTERMEDIATE', NULL, NULL, 'Hold dumbbells at chin level with palms facing you, rotate palms outward while pressing overhead to full extension, reverse the rotation on the way down.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Arnold Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Wrist Curl', 'STRENGTH', '{FOREARMS}', '{DUMBBELL}', 'BEGINNER', NULL, NULL, 'Sit with forearms resting on thighs and palms facing up holding dumbbells, curl wrists upward by flexing the forearms, lower fully and repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Wrist Curl' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Swing', 'STRENGTH', '{GLUTES,HAMSTRINGS,CORE,SHOULDERS}', '{DUMBBELL}', 'INTERMEDIATE', NULL, NULL, 'Hold one dumbbell with both hands, hinge at the hips to swing it back between the legs, then drive hips forward explosively to swing it to chest height.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Swing' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Floor Press', 'STRENGTH', '{CHEST,TRICEPS}', '{DUMBBELL}', 'BEGINNER', NULL, NULL, 'Lie on the floor holding dumbbells at chest level, press them to full extension above the chest, lower until elbows touch the floor and repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Floor Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Lateral Lunge', 'STRENGTH', '{LEGS,GLUTES,ADDUCTORS}', '{DUMBBELL}', 'INTERMEDIATE', NULL, NULL, 'Hold dumbbells at sides, step one foot wide to the side and bend that knee while keeping the opposite leg straight, push back to center and alternate.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Lateral Lunge' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Sumo Squat', 'STRENGTH', '{LEGS,GLUTES,ADDUCTORS}', '{DUMBBELL}', 'BEGINNER', NULL, NULL, 'Stand with feet wider than shoulder-width and toes turned out, hold one dumbbell between the legs, squat down keeping chest tall, drive through heels to stand.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Sumo Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Dumbbell Snatch', 'STRENGTH', '{SHOULDERS,BACK,LEGS,CORE}', '{DUMBBELL}', 'ADVANCED', NULL, NULL, 'Start with dumbbell on the floor, pull it explosively from the ground to overhead in one fluid movement by extending hips, knees, and ankles while keeping it close to the body.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Dumbbell Snatch' AND is_system = TRUE);

-- ----------------------------------------
-- BARBELL (~13)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Hip Thrust', 'STRENGTH', '{GLUTES,HAMSTRINGS}', '{BARBELL,BENCH}', 'INTERMEDIATE', NULL, NULL, 'Sit with upper back against a bench, barbell across hips, drive hips upward by squeezing glutes until body forms a straight line, lower and repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Hip Thrust' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Shrug', 'STRENGTH', '{TRAPS}', '{BARBELL}', 'BEGINNER', NULL, NULL, 'Hold a barbell in front of thighs with arms straight, elevate shoulders straight up toward ears as high as possible, hold briefly then lower slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Shrug' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Front Squat', 'STRENGTH', '{LEGS,CORE,SHOULDERS}', '{BARBELL,SQUAT_RACK}', 'ADVANCED', NULL, NULL, 'Rest the barbell on the front deltoids with elbows high, squat down keeping an upright torso until thighs are parallel, drive back up to full extension.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Front Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Lunge', 'STRENGTH', '{LEGS,GLUTES}', '{BARBELL,SQUAT_RACK}', 'INTERMEDIATE', NULL, NULL, 'With a barbell on your upper back, step forward into a lunge lowering the rear knee toward the floor, push through the front heel to return to standing, alternate legs.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Lunge' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Good Morning', 'STRENGTH', '{HAMSTRINGS,GLUTES,BACK}', '{BARBELL}', 'INTERMEDIATE', NULL, NULL, 'Place a barbell on your upper traps, hinge forward at the hips with a slight knee bend until torso is near parallel to the floor, drive hips forward to return upright.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Good Morning' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Sumo Deadlift', 'STRENGTH', '{LEGS,GLUTES,BACK,CORE}', '{BARBELL}', 'INTERMEDIATE', NULL, NULL, 'Stand with feet wide and toes turned out, grip bar inside the knees, keep chest tall and push the floor away to stand, keeping bar close to the body throughout.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Sumo Deadlift' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Close Grip Bench Press', 'STRENGTH', '{TRICEPS,CHEST}', '{BARBELL,BENCH}', 'INTERMEDIATE', NULL, NULL, 'Lie on a bench and grip the barbell with hands shoulder-width apart, lower to the lower chest keeping elbows close to the torso, press back to full extension.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Close Grip Bench Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Upright Row', 'STRENGTH', '{SHOULDERS,TRAPS}', '{BARBELL}', 'INTERMEDIATE', NULL, NULL, 'Hold a barbell at hip level with an overhand grip, pull it straight up along the body to chin height leading with the elbows, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Upright Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Reverse Curl', 'STRENGTH', '{BICEPS,FOREARMS}', '{BARBELL}', 'BEGINNER', NULL, NULL, 'Hold a barbell with an overhand grip at thigh level, curl to shoulder height keeping wrists neutral, lower under control emphasizing the brachialis.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Reverse Curl' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Pendlay Row', 'STRENGTH', '{BACK,BICEPS}', '{BARBELL}', 'ADVANCED', NULL, NULL, 'Start with bar on the floor, torso parallel to the ground, explosively pull the bar to the lower chest and return it fully to the floor each rep, maintaining a strict horizontal torso.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Pendlay Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'T-Bar Row', 'STRENGTH', '{BACK,BICEPS}', '{BARBELL,MACHINE}', 'INTERMEDIATE', NULL, NULL, 'Straddle a landmine barbell or use a T-bar row machine, hinge forward and row the handles to the lower chest, squeeze shoulder blades at the top, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'T-Bar Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Zercher Squat', 'STRENGTH', '{LEGS,CORE,BACK}', '{BARBELL}', 'ADVANCED', NULL, NULL, 'Hold the barbell in the crooks of your elbows at waist height, squat down keeping an upright torso, drive through heels to return to standing.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Zercher Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Barbell Wrist Curl', 'STRENGTH', '{FOREARMS}', '{BARBELL}', 'BEGINNER', NULL, NULL, 'Sit with forearms resting on thighs and palms up holding a barbell, curl wrists upward by flexing the forearms fully, lower completely and repeat.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Barbell Wrist Curl' AND is_system = TRUE);

-- ----------------------------------------
-- CABLE (~12)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Lateral Raise', 'STRENGTH', '{SHOULDERS}', '{CABLE}', 'BEGINNER', NULL, NULL, 'Stand beside a low cable pulley with the handle in the far hand, raise the arm out to shoulder height with a slight elbow bend, lower slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Lateral Raise' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Curl', 'STRENGTH', '{BICEPS}', '{CABLE}', 'BEGINNER', NULL, NULL, 'Stand at a low cable pulley, grip the bar or handle with an underhand grip, curl to shoulder height keeping elbows at sides, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Curl' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Tricep Extension', 'STRENGTH', '{TRICEPS}', '{CABLE}', 'BEGINNER', NULL, NULL, 'Stand at a high pulley with a rope or bar attachment, keep upper arms at sides and extend forearms down to full lockout, return slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Tricep Extension' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Woodchop', 'STRENGTH', '{CORE,SHOULDERS}', '{CABLE}', 'INTERMEDIATE', NULL, NULL, 'Set cable at shoulder height, grip the handle with both hands, rotate and pull in a diagonal downward arc across the body, control the return.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Woodchop' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Pull Through', 'STRENGTH', '{GLUTES,HAMSTRINGS}', '{CABLE}', 'BEGINNER', NULL, NULL, 'Face away from a low cable with the rope between your legs, hinge forward at the hips until torso is near parallel, drive hips forward to stand tall and squeeze glutes.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Pull Through' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Crunch', 'STRENGTH', '{CORE}', '{CABLE}', 'BEGINNER', NULL, NULL, 'Kneel at a high pulley holding the rope at your forehead, crunch downward by contracting the abs until elbows meet the knees, return under control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Crunch' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Crossover', 'STRENGTH', '{CHEST}', '{CABLE}', 'INTERMEDIATE', NULL, NULL, 'Stand between two high cable pulleys, pull the handles downward and together in a wide arc in front of your chest, squeeze at the midpoint then return.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Crossover' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Kickback', 'STRENGTH', '{GLUTES}', '{CABLE}', 'BEGINNER', NULL, NULL, 'Attach an ankle strap to a low cable, face the machine and hinge slightly forward, kick the attached leg straight back squeezing the glute at the top, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Kickback' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Upright Row', 'STRENGTH', '{SHOULDERS,TRAPS}', '{CABLE}', 'BEGINNER', NULL, NULL, 'Stand at a low cable pulley, pull the bar straight up along the body to chin height leading with the elbows flared wide, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Upright Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Reverse Fly', 'STRENGTH', '{BACK,SHOULDERS}', '{CABLE}', 'BEGINNER', NULL, NULL, 'Stand between two low cable pulleys with crossed handles, pull arms apart in a wide arc to shoulder height squeezing the rear delts, return slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Reverse Fly' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Front Raise', 'STRENGTH', '{SHOULDERS}', '{CABLE}', 'BEGINNER', NULL, NULL, 'Stand at a low cable pulley with the handle in front, raise both arms straight forward to shoulder height, lower slowly and with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Front Raise' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Cable Hammer Curl', 'STRENGTH', '{BICEPS,FOREARMS}', '{CABLE}', 'BEGINNER', NULL, NULL, 'Attach a rope to a low cable, grip with a neutral (hammer) grip, curl rope to shoulder height keeping elbows at sides, lower with full control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Cable Hammer Curl' AND is_system = TRUE);

-- ----------------------------------------
-- MACHINE (~13)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Smith Machine Squat', 'STRENGTH', '{LEGS,GLUTES}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Position the bar on the Smith machine across your upper traps, unhook and squat to parallel keeping knees tracking over toes, press back up and re-rack.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Smith Machine Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Hack Squat', 'STRENGTH', '{LEGS,GLUTES}', '{MACHINE}', 'INTERMEDIATE', NULL, NULL, 'Position yourself in the hack squat machine with shoulders and back against the pads, lower until thighs are at or below parallel, press back up without locking out knees.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Hack Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Chest Press Machine', 'STRENGTH', '{CHEST,TRICEPS,SHOULDERS}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Sit in the chest press machine with back flat against the pad, grip the handles at chest level and press to full extension, return slowly to the start.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Chest Press Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Pec Deck', 'STRENGTH', '{CHEST}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Sit in the pec deck machine with elbows on the arm pads, bring the pads together in front of your chest squeezing the pecs, return with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Pec Deck' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Shoulder Press Machine', 'STRENGTH', '{SHOULDERS,TRICEPS}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Sit in the shoulder press machine with back supported, grip handles at shoulder height and press overhead to full extension, lower slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Shoulder Press Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Assisted Pull Up Machine', 'STRENGTH', '{BACK,BICEPS}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Kneel or stand on the assisted pull-up platform, grip the bar overhead, pull chest to the bar while the counterweight reduces effective load, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Assisted Pull Up Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Seated Row Machine', 'STRENGTH', '{BACK,BICEPS}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Sit at the seated row machine with chest against the pad, grip the handles and row to your midsection driving elbows back, squeeze shoulder blades and return.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Seated Row Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Preacher Curl Machine', 'STRENGTH', '{BICEPS}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Sit at the preacher curl machine with upper arms flat on the angled pad, curl the handles up to full contraction and lower fully for maximum stretch.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Preacher Curl Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Leg Curl Machine', 'STRENGTH', '{HAMSTRINGS}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Lie or sit in the leg curl machine with pad positioned just above the heels, curl legs toward glutes against resistance, lower slowly to the starting position.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Leg Curl Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Leg Extension Machine', 'STRENGTH', '{QUADRICEPS}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Sit in the leg extension machine with pad on the shins just above the ankle, extend legs to full lockout squeezing the quads, lower slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Leg Extension Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Hip Abductor Machine', 'STRENGTH', '{GLUTES,HIP_ABDUCTORS}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Sit in the hip abductor machine with pads on the outer thighs, push legs outward against resistance as far as possible, return slowly with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Hip Abductor Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Hip Adductor Machine', 'STRENGTH', '{ADDUCTORS}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Sit in the hip adductor machine with pads on the inner thighs, squeeze legs together against resistance, return slowly to the starting position.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Hip Adductor Machine' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Calf Raise Machine', 'STRENGTH', '{CALVES}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Stand in the calf raise machine with shoulders under the pads and balls of feet on the platform, raise onto toes as high as possible, lower heels below platform level for a full stretch.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Calf Raise Machine' AND is_system = TRUE);

-- ----------------------------------------
-- RESISTANCE BANDS (~8)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Pull Apart', 'STRENGTH', '{BACK,SHOULDERS}', '{RESISTANCE_BAND}', 'BEGINNER', NULL, NULL, 'Hold a resistance band with both hands at shoulder height, pull it apart by extending arms wide until band touches your chest, return slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Pull Apart' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Face Pull', 'STRENGTH', '{SHOULDERS,BACK}', '{RESISTANCE_BAND}', 'BEGINNER', NULL, NULL, 'Anchor a band at face height, pull it toward your face with elbows flared high and externally rotate at the end position, return under control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Face Pull' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Squat', 'STRENGTH', '{LEGS,GLUTES}', '{RESISTANCE_BAND}', 'BEGINNER', NULL, NULL, 'Stand on a resistance band with feet shoulder-width, hold the ends at shoulder level, squat down to parallel keeping chest tall, drive back up to standing.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Squat' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Good Morning', 'STRENGTH', '{HAMSTRINGS,GLUTES,BACK}', '{RESISTANCE_BAND}', 'BEGINNER', NULL, NULL, 'Stand on a band with feet hip-width and loop it over the back of your neck, hinge forward at the hips with slight knee bend, drive hips forward to return upright.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Good Morning' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Chest Press', 'STRENGTH', '{CHEST,TRICEPS,SHOULDERS}', '{RESISTANCE_BAND}', 'BEGINNER', NULL, NULL, 'Anchor a band behind you at chest height, grip the ends at chest level, press forward to full arm extension, return slowly against the band tension.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Chest Press' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Row', 'STRENGTH', '{BACK,BICEPS}', '{RESISTANCE_BAND}', 'BEGINNER', NULL, NULL, 'Anchor a band in front of you at waist height, grip the ends with both hands and row them toward your midsection driving elbows back, return slowly.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Row' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Lateral Walk', 'STRENGTH', '{GLUTES,HIP_ABDUCTORS}', '{RESISTANCE_BAND}', 'BEGINNER', NULL, NULL, 'Place a band around your ankles or just above the knees, assume a quarter-squat position and step laterally keeping tension in the band throughout.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Lateral Walk' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Band Bicep Curl', 'STRENGTH', '{BICEPS}', '{RESISTANCE_BAND}', 'BEGINNER', NULL, NULL, 'Stand on a resistance band with feet hip-width, grip the ends with palms facing up, curl hands toward shoulders keeping elbows at sides, lower with control.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Band Bicep Curl' AND is_system = TRUE);

-- ----------------------------------------
-- CARDIO / OTHER (~5)
-- ----------------------------------------

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Battle Ropes', 'CARDIO', '{CARDIO,SHOULDERS,CORE,ARMS}', '{BATTLE_ROPES}', 'INTERMEDIATE', NULL, NULL, 'Hold one rope end in each hand, create alternating or simultaneous waves by moving arms up and down rapidly, maintain a stable athletic stance for the duration.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Battle Ropes' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Sled Push', 'CARDIO', '{CARDIO,LEGS,GLUTES,CORE}', '{SLED}', 'INTERMEDIATE', NULL, NULL, 'Load a sled to appropriate weight, grip the handles and drive it forward by pushing explosively through the legs, lean forward and take short powerful steps.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Sled Push' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Box Step Up', 'STRENGTH', '{LEGS,GLUTES}', '{BODYWEIGHT,PLYO_BOX}', 'BEGINNER', NULL, NULL, 'Stand facing a box or step, place one foot fully on the surface, press through that heel to step up bringing the trailing leg to meet it, step back down and alternate.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Box Step Up' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Assault Bike', 'CARDIO', '{CARDIO,LEGS,ARMS,CORE}', '{MACHINE}', 'INTERMEDIATE', NULL, NULL, 'Sit on the assault bike, grip the moving handles and pedal as hard as possible using both arms and legs simultaneously, regulate intensity by adjusting speed.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Assault Bike' AND is_system = TRUE);

INSERT INTO exercise_catalog (name, exercise_type, muscle_groups, equipment_required, difficulty_level, video_url, thumbnail_url, instructions, is_system, created_by_user_id)
SELECT 'Elliptical', 'CARDIO', '{CARDIO,LEGS,ARMS}', '{MACHINE}', 'BEGINNER', NULL, NULL, 'Step onto the elliptical and grip the handles, push and pull with arms while striding with legs in an elliptical motion, maintain an upright posture at the target heart rate.', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM exercise_catalog WHERE name = 'Elliptical' AND is_system = TRUE);
